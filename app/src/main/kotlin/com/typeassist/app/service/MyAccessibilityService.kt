package com.typeassist.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ProgressBar
import java.util.regex.Pattern
import android.widget.Toast
import com.typeassist.app.api.GeminiApiClient
import com.typeassist.app.api.CloudflareApiClient
import com.typeassist.app.data.HistoryManager
import com.typeassist.app.data.AppConfig
import com.google.gson.Gson
import okhttp3.*

class MyAccessibilityService : AccessibilityService() {

    private val client = OkHttpClient()
    private val geminiApiClient = GeminiApiClient(client)
    private val cloudflareApiClient = CloudflareApiClient(client)
    private var windowManager: WindowManager? = null
    
    // --- UI Elements ---
    private var loadingView: FrameLayout? = null
    private var undoView: FrameLayout? = null
    private var previewView: FrameLayout? = null // Holds the preview dialog wrapper
    
    // --- Undo Cache ---
    private var lastNode: AccessibilityNodeInfo? = null
    private var originalTextCache: String = ""
    private var undoCacheTimestamp: Long = 0L
    private val undoHandler = Handler(Looper.getMainLooper())
    private val hideUndoRunnable = Runnable { hideUndoButton() }
    private val hidePreviewRunnable = Runnable { hidePreviewDialog() }

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        if (event.packageName?.toString() == packageName) {
            val prefs = getSharedPreferences("GeminiConfig", Context.MODE_PRIVATE)
            val isTesting = prefs.getBoolean("is_testing_active", false)
            if (!isTesting) return
        }

        if (event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            val inputNode = event.source ?: return
            var currentText = inputNode.text?.toString() ?: ""
            if (currentText.isEmpty() && event.text != null && event.text.isNotEmpty()) {
                currentText = event.text.joinToString("")
            }

            val prefs = getSharedPreferences("GeminiConfig", Context.MODE_PRIVATE)
            val configJson = prefs.getString("config_json", null) ?: return

            try {
                val gson = Gson()
                val config = gson.fromJson(configJson, AppConfig::class.java)
                
                if (!config.isAppEnabled) {
                    return
                }

                // --- 0. Global Inline Transformation (...instruction...) ---
                val globalTransformRegex = Pattern.compile("(?s)(.*)\\.\\.\\.(.+?)\\.\\.\\.$")
                val globalMatcher = globalTransformRegex.matcher(currentText)
                if (globalMatcher.find()) {
                    val contextText = globalMatcher.group(1) ?: ""
                    val instruction = globalMatcher.group(2) ?: ""

                    if (contextText.isNotBlank() && instruction.isNotBlank()) {
                        originalTextCache = currentText
                        HistoryManager.add(originalTextCache)
                        lastNode = inputNode
                        undoCacheTimestamp = System.currentTimeMillis()

                        showLoading(config)
                        hideUndoButton()

                        val systemPrompt = "Rewrite the following text according to this instruction: $instruction. Return ONLY the rewritten text, no explanations, no chat."
                        
                        performAICall(config, systemPrompt, contextText) { result ->
                            hideLoading()
                            result.onSuccess { aiText ->
                                val wordCount = aiText.split("\\s+".toRegex()).size
                                if (wordCount > 15 && config.enablePreviewDialog) {
                                    showPreviewDialog(aiText) {
                                        pasteText(inputNode, aiText)
                                        showUndoButton(config)
                                    }
                                } else {
                                    pasteText(inputNode, aiText)
                                    showUndoButton(config)
                                }
                            }.onFailure {
                                showToast(it.message ?: "Unknown error")
                            }
                        }
                        return
                    }
                }

                // --- Snippets Logic ---
                val snippetPrefix = config.snippetTriggerPrefix
                val saveSnippetPattern = config.saveSnippetPattern
                val snippets = config.snippets

                // 1. Check for Snippet Usage
                for (s in snippets) {
                    val fullTrigger = snippetPrefix + s.trigger

                    if (currentText.endsWith(fullTrigger)) {
                        val newText = currentText.substring(0, currentText.length - fullTrigger.length) + s.content
                        pasteText(inputNode, newText)
                        return
                    }
                }

                // 2. Check for Snippet Saving
                val saveMatcher = Pattern.compile(buildSaveSnippetRegex(saveSnippetPattern)).matcher(currentText)
                if (saveMatcher.find()) {
                    val fullMatch = saveMatcher.group(0) ?: ""
                    val newTrigger = saveMatcher.group(1)?.trim() ?: ""
                    val newContent = saveMatcher.group(2)?.trim() ?: ""

                    if (newTrigger.isNotEmpty() && newContent.isNotEmpty()) {
                        // Update Snippets
                        config.snippets.removeIf { it.trigger == newTrigger }
                        config.snippets.add(com.typeassist.app.data.Snippet(newTrigger, newContent))

                        // Save Config
                        prefs.edit().putString("config_json", gson.toJson(config)).apply()

                        // Remove command from text
                        val cleanText = currentText.replace(fullMatch, newContent)
                        pasteText(inputNode, cleanText)
                        showToast("Snippet '$newTrigger' saved!")
                        return
                    }
                }

                // --- Utility Belt ---
                // 1. Calculator: (.c: expression)
                findBalancedCommand(currentText, "(.c:")?.let { (fullMatch, expr) ->
                    val result = com.typeassist.app.utils.UtilityBelt.evaluateMath(expr)

                    originalTextCache = currentText
                    HistoryManager.add(originalTextCache)
                    lastNode = inputNode
                    undoCacheTimestamp = System.currentTimeMillis()

                    val newText = currentText.replace(fullMatch, result)
                    pasteText(inputNode, newText)
                    showUndoButton(config)
                    return
                }

                // 2. Suffix Utilities: .now, .date, .pass
                var utilityResult: String? = null
                var triggerLen = 0
                
                if (currentText.endsWith(".now")) {
                    utilityResult = com.typeassist.app.utils.UtilityBelt.getTime()
                    triggerLen = 4
                } else if (currentText.endsWith(".date")) {
                    utilityResult = com.typeassist.app.utils.UtilityBelt.getDate()
                    triggerLen = 5
                } else if (currentText.endsWith(".pass")) {
                    utilityResult = com.typeassist.app.utils.UtilityBelt.generatePassword()
                    triggerLen = 5
                }

                if (utilityResult != null) {
                    originalTextCache = currentText
                    HistoryManager.add(originalTextCache)
                    lastNode = inputNode
                    undoCacheTimestamp = System.currentTimeMillis()

                    val newText = currentText.substring(0, currentText.length - triggerLen) + utilityResult
                    pasteText(inputNode, newText)
                    showUndoButton(config)
                    return
                }

                val undoCommandPattern = config.undoCommandPattern.trim()

                // Handle .undo command
                val timeSinceCache = System.currentTimeMillis() - undoCacheTimestamp
                if (currentText.endsWith(undoCommandPattern) && originalTextCache.isNotEmpty() && timeSinceCache < 120000) { // 2 minutes
                    pasteText(inputNode, originalTextCache) // Paste into the current node
                    return // Consume the event
                }
                
                val triggers = config.triggers
                val inlineCommands = config.inlineCommands

                // --- Process Inline Commands ---
                for (inlineCommand in inlineCommands) {
                    val inlinePattern = inlineCommand.pattern
                    val inlinePromptTemplate = inlineCommand.prompt

                    val regexPattern = Pattern.compile(buildRegexFromInlinePattern(inlinePattern))
                    val matcher = regexPattern.matcher(currentText)

                    if (matcher.find()) {
                        val fullMatchedString = matcher.group(0) ?: continue
                        val userPrompt = matcher.group(1) ?: continue // '%' is replaced by this group

                        originalTextCache = currentText // Store the full current text for undo
                        HistoryManager.add(originalTextCache) // Save to history
                        lastNode = inputNode

                        showLoading(config)
                        hideUndoButton()

                        performAICall(config, inlinePromptTemplate, userPrompt) { result ->
                            hideLoading()
                            result.onSuccess { aiText ->
                                val wordCount = aiText.split("\\s+".toRegex()).size
                                if (wordCount > 15 && config.enablePreviewDialog) {
                                    showPreviewDialog(aiText) {
                                        val newText = currentText.replace(fullMatchedString, aiText)
                                        pasteText(inputNode, newText)
                                        showUndoButton(config)
                                    }
                                } else {
                                    val newText = currentText.replace(fullMatchedString, aiText)
                                    pasteText(inputNode, newText)
                                    showUndoButton(config)
                                }
                            }.onFailure {
                                showToast(it.message ?: "Unknown error")
                            }
                        }
                        return // Consume the event after processing one inline command
                    }
                }

                // --- Process Trailing Triggers (Old Logic) ---
                for (trigger in triggers) {
                    val pattern = trigger.pattern
                    val prompt = trigger.prompt

                    if (isTriggerValid(currentText, pattern)) {
                        val textToProcess = currentText.substring(0, currentText.length - pattern.length).trim()
                        
                        if (textToProcess.length > 1) {
                            originalTextCache = textToProcess
                            lastNode = inputNode
                            undoCacheTimestamp = System.currentTimeMillis()
                            HistoryManager.add(originalTextCache) // Save to history

                            showLoading(config)
                            hideUndoButton() 

                            performAICall(config, prompt, textToProcess) { result ->
                                hideLoading()
                                result.onSuccess { aiText ->
                                    val wordCount = aiText.split("\\s+".toRegex()).size
                                    if (wordCount > 15 && config.enablePreviewDialog) {
                                        showPreviewDialog(aiText) {
                                            pasteText(inputNode, aiText)
                                            showUndoButton(config)
                                        }
                                    } else {
                                        pasteText(inputNode, aiText)
                                        showUndoButton(config)
                                    }
                                }.onFailure {
                                    showToast(it.message ?: "Unknown error")
                                }
                            }
                        }
                        return 
                    }
                }
            } catch (e: Exception) {
                // Silent fail
            }
        }
    }

    private fun performAICall(config: AppConfig, prompt: String, userText: String, callback: (Result<String>) -> Unit) {
        if (config.provider == "cloudflare") {
            cloudflareApiClient.callCloudflare(
                config.cloudflareConfig.accountId,
                config.cloudflareConfig.apiToken,
                config.cloudflareConfig.model,
                prompt,
                userText,
                callback
            )
        } else {
            geminiApiClient.callGemini(
                config.apiKey,
                config.model,
                prompt,
                userText,
                config.generationConfig.temperature,
                config.generationConfig.topP,
                callback
            )
        }
    }

    private fun isTriggerValid(text: String, trigger: String): Boolean {
        if (!text.endsWith(trigger)) {
            return false
        }

        val triggerStartIndex = text.length - trigger.length
        if (triggerStartIndex > 0) {
            val charBefore = text[triggerStartIndex - 1]
            if (!charBefore.isWhitespace()) {
                return false
            }
        }
        return true
    }

    private fun showLoading(config: AppConfig) {
        if (!config.enableLoadingOverlay) return
        Handler(Looper.getMainLooper()).post {
            if (loadingView != null) return@post
            loadingView = FrameLayout(this).apply {
                setBackgroundColor(0x77000000.toInt())
                setPadding(30, 30, 30, 30)
                background = GradientDrawable().apply { setColor(0x99000000.toInt()); cornerRadius = 40f }
            }
            val progressBar = ProgressBar(this)
            progressBar.indeterminateTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
            loadingView?.addView(progressBar)
            val params = WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT)
            params.gravity = Gravity.CENTER
            try { windowManager?.addView(loadingView, params) } catch (e: Exception) {}
        }
    }

    private fun hideLoading() {
        Handler(Looper.getMainLooper()).post {
            if (loadingView != null) { try { windowManager?.removeView(loadingView); loadingView = null } catch (e: Exception) {} }
        }
    }

    private fun showUndoButton(config: AppConfig) {
        if (!config.enableUndoOverlay) return
        Handler(Looper.getMainLooper()).post {
            if (undoView != null) return@post
            undoView = FrameLayout(this)
            val btn = Button(this).apply {
                text = "UNDO"
                textSize = 14f
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply { setColor(0xEE333333.toInt()); cornerRadius = 50f; setStroke(2, Color.WHITE) }
                setOnClickListener { performUndo() }
            }
            undoView?.addView(btn)
            val params = WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT)
            params.gravity = Gravity.CENTER
            try { windowManager?.addView(undoView, params); undoHandler.postDelayed(hideUndoRunnable, 5000) } catch (e: Exception) {}
        }
    }

    private fun hideUndoButton() {
        undoHandler.removeCallbacks(hideUndoRunnable)
        Handler(Looper.getMainLooper()).post {
            if (undoView != null) { try { windowManager?.removeView(undoView); undoView = null } catch (e: Exception) {} }
        }
    }

    private fun showPreviewDialog(text: String, onInsert: () -> Unit) {
        Handler(Looper.getMainLooper()).post {
            if (previewView != null) hidePreviewDialog()
            
            // Check Dark Mode
            val nightModeFlags = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
            val isDarkMode = nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES
            
            val cardBgColor = if (isDarkMode) 0xFF1F2937.toInt() else 0xFFFFFFFF.toInt()
            val primaryTextColor = if (isDarkMode) Color.WHITE else Color.BLACK
            val secondaryTextColor = if (isDarkMode) 0xFFD1D5DB.toInt() else 0xFF333333.toInt()
            val discardTextColor = if (isDarkMode) 0xFF9CA3AF.toInt() else Color.GRAY
            val insertTextColor = if (isDarkMode) 0xFF818CF8.toInt() else 0xFF4F46E5.toInt()

            // The Card (As Root View)
            val card = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                setPadding(40, 40, 40, 40)
                background = GradientDrawable().apply { 
                    setColor(cardBgColor)
                    cornerRadius = 32f 
                }
                // Handle outside touches (optional if we use FLAG_WATCH_OUTSIDE_TOUCH correctly)
                isClickable = true
            }

            val title = android.widget.TextView(this).apply {
                this.text = "Preview Long Response"
                textSize = 18f
                setTextColor(primaryTextColor)
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(0, 0, 0, 20)
            }
            card.addView(title)

            val scrollView = android.widget.ScrollView(this).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 
                    0 
                ).apply { weight = 1f }
            }
            // Constrain height
            scrollView.layoutParams.height = (resources.displayMetrics.heightPixels * 0.4).toInt()
            
            val contentText = android.widget.TextView(this).apply {
                this.text = text
                textSize = 14f
                setTextColor(secondaryTextColor)
            }
            scrollView.addView(contentText)
            card.addView(scrollView)

            val btnRow = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                gravity = Gravity.END
                setPadding(0, 30, 0, 0)
            }

            val discardBtn = Button(this).apply {
                this.text = "Discard"
                background = null
                setTextColor(discardTextColor)
                setOnClickListener { hidePreviewDialog() }
            }

            val insertBtn = Button(this).apply {
                this.text = "Insert"
                background = null
                setTextColor(insertTextColor)
                setTypeface(null, android.graphics.Typeface.BOLD)
                setOnClickListener { 
                    onInsert()
                    hidePreviewDialog()
                }
            }

            btnRow.addView(discardBtn)
            btnRow.addView(insertBtn)
            card.addView(btnRow)

            // Wrap in a FrameLayout that does NOT fill screen, but wraps card.
            // However, to use FLAG_DIM_BEHIND effectively, the window can be small.
            // But to use FLAG_WATCH_OUTSIDE_TOUCH, we need to be small.
            
            previewView = FrameLayout(this)
            previewView?.addView(card)

            val rootParams = WindowManager.LayoutParams(
                (resources.displayMetrics.widthPixels * 0.85).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                // Critical Flags:
                // NOT_FOCUSABLE: Don't steal keys (Home/Back/Recents pass through).
                // WATCH_OUTSIDE_TOUCH: Get events when user touches outside window.
                // NOT_TOUCH_MODAL: Allow outside touches to go to underlying apps.
                // DIM_BEHIND: System handles the dimming.
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
                dimAmount = 0.5f
            }
            
            // Set touch listener on the wrapper to catch the outside touch event provided by the flag
            previewView?.setOnTouchListener { v, event ->
                if (event.action == android.view.MotionEvent.ACTION_OUTSIDE) {
                    hidePreviewDialog()
                    true
                } else {
                    false
                }
            }

            try { 
                windowManager?.addView(previewView, rootParams) 
                undoHandler.postDelayed(hidePreviewRunnable, 30000)
            } catch (e: Exception) {}
        }
    }

    private fun hidePreviewDialog() {
        undoHandler.removeCallbacks(hidePreviewRunnable)
        Handler(Looper.getMainLooper()).post {
            if (previewView != null) { try { windowManager?.removeView(previewView); previewView = null } catch (e: Exception) {} }
        }
    }

    private fun performUndo() {
        val timeSinceCache = System.currentTimeMillis() - undoCacheTimestamp
        if (lastNode != null && originalTextCache.isNotEmpty() && timeSinceCache < 120000) { // 2 minutes
            if (lastNode!!.refresh()) {
                pasteText(lastNode!!, originalTextCache)
                showToast("Undone!")
            }
        }
        hideUndoButton()
    }

    private fun pasteText(node: AccessibilityNodeInfo, text: String) {
        val arguments = Bundle()
        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        node.refresh()
        node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post { Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show() }
    }

    private fun buildRegexFromInlinePattern(inlinePattern: String): String {
        // Escape special regex characters, except for the placeholder '%'
        val escapedPattern = Pattern.quote(inlinePattern).replace("%", "\\E(.+?)\\Q")
            .replace("\\Q\\E", "") // Clean up empty escapes
        return escapedPattern
    }

    private fun buildSaveSnippetRegex(pattern: String): String {
        // Pattern has two % placeholders: first for name, second for content
        // e.g. "(.save:%:%)" -> "\Q(.save:\E(.+?)\Q:\E(.+?)\Q)\E"
        val parts = pattern.split("%", limit = 3)
        if (parts.size != 3) return Pattern.quote(pattern) // Fallback if invalid pattern

        val sb = StringBuilder()
        sb.append(Pattern.quote(parts[0]))
        sb.append("(.+?)") // Group 1: Name
        sb.append(Pattern.quote(parts[1]))
        sb.append("(.+?)") // Group 2: Content
        sb.append(Pattern.quote(parts[2]))
        
        return sb.toString()
    }

    private fun findBalancedCommand(text: String, startPattern: String): Pair<String, String>? {
        val startIndex = text.lastIndexOf(startPattern)
        if (startIndex == -1) return null

        val contentStartIndex = startIndex + startPattern.length
        var balance = 0
        var endIndex = -1

        for (i in contentStartIndex until text.length) {
            when (text[i]) {
                '(' -> balance++
                ')' -> {
                    if (balance == 0) {
                        endIndex = i
                        break
                    }
                    balance--
                }
            }
        }

        if (endIndex != -1) {
            val fullMatch = text.substring(startIndex, endIndex + 1)
            val expression = text.substring(contentStartIndex, endIndex)
            return Pair(fullMatch, expression)
        }

        return null
    }

    override fun onInterrupt() { hideLoading(); hideUndoButton(); hidePreviewDialog() }
}