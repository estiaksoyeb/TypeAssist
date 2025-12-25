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
import android.text.InputType
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ProgressBar
import java.util.regex.Pattern
import android.widget.Toast
import com.typeassist.app.api.GeminiApiClient
import com.typeassist.app.data.HistoryManager
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject

class MyAccessibilityService : AccessibilityService() {

    private val client = OkHttpClient()
    private val geminiApiClient = GeminiApiClient(client)
    private var windowManager: WindowManager? = null
    
    // --- UI Elements ---
    private var loadingView: FrameLayout? = null
    private var undoView: FrameLayout? = null
    
    // --- Undo Cache ---
    private var lastNode: AccessibilityNodeInfo? = null
    private var originalTextCache: String = ""
    private var undoCacheTimestamp: Long = 0L
    private val undoHandler = Handler(Looper.getMainLooper())
    private val hideUndoRunnable = Runnable { hideUndoButton() }

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
                val configObj = JSONObject(configJson)
                
                if (configObj.has("isAppEnabled") && !configObj.getBoolean("isAppEnabled")) {
                    return
                }

                val apiKey = configObj.getString("apiKey").trim()
                val model = configObj.getString("model").trim()
                
                // --- Snippets Logic ---
                val snippetPrefix = configObj.optString("snippetTriggerPrefix", "ta#")
                val saveSnippetPattern = configObj.optString("saveSnippetPattern", "(.save:%:%)")
                val snippets = configObj.optJSONArray("snippets") ?: JSONArray()

                // 1. Check for Snippet Usage
                for (i in 0 until snippets.length()) {
                    val s = snippets.getJSONObject(i)
                    val trig = s.getString("trigger")
                    val content = s.getString("content")
                    val fullTrigger = snippetPrefix + trig

                    if (currentText.endsWith(fullTrigger)) {
                        val newText = currentText.substring(0, currentText.length - fullTrigger.length) + content
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
                        // Remove existing if exists
                        val newSnippets = JSONArray()
                        for (i in 0 until snippets.length()) {
                            val s = snippets.getJSONObject(i)
                            if (s.getString("trigger") != newTrigger) {
                                newSnippets.put(s)
                            }
                        }
                        // Add new
                        val newSnippet = JSONObject()
                        newSnippet.put("trigger", newTrigger)
                        newSnippet.put("content", newContent)
                        newSnippets.put(newSnippet)

                        // Save Config
                        configObj.put("snippets", newSnippets)
                        prefs.edit().putString("config_json", configObj.toString()).apply()

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
                    showUndoButton()
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
                    showUndoButton()
                    return
                }

                val undoCommandPattern = configObj.getString("undoCommandPattern").trim()

                // Handle .undo command
                val timeSinceCache = System.currentTimeMillis() - undoCacheTimestamp
                if (currentText.endsWith(undoCommandPattern) && originalTextCache.isNotEmpty() && timeSinceCache < 120000) { // 2 minutes
                    pasteText(inputNode, originalTextCache) // Paste into the current node
                    return // Consume the event
                }
                
                var temp = 0.2
                var topP = 0.95
                if (configObj.has("generationConfig")) {
                    val genConfig = configObj.getJSONObject("generationConfig")
                    temp = genConfig.optDouble("temperature", 0.2)
                    topP = genConfig.optDouble("topP", 0.95)
                }

                val triggers = configObj.getJSONArray("triggers")
                val inlineCommands = configObj.getJSONArray("inlineCommands")

                // --- Process Inline Commands ---
                for (i in 0 until inlineCommands.length()) {
                    val inlineCommandObj = inlineCommands.getJSONObject(i)
                    val inlinePattern = inlineCommandObj.getString("pattern")
                    val inlinePromptTemplate = inlineCommandObj.getString("prompt")

                    val regexPattern = Pattern.compile(buildRegexFromInlinePattern(inlinePattern))
                    val matcher = regexPattern.matcher(currentText)

                    if (matcher.find()) {
                        val fullMatchedString = matcher.group(0) ?: continue
                        val userPrompt = matcher.group(1) ?: continue // '%' is replaced by this group

                        originalTextCache = currentText // Store the full current text for undo
                        HistoryManager.add(originalTextCache) // Save to history
                        lastNode = inputNode

                        showLoading()
                        hideUndoButton()

                        geminiApiClient.callGemini(apiKey, model, inlinePromptTemplate, userPrompt, temp, topP) { result ->
                            hideLoading()
                            result.onSuccess {
                                val newText = currentText.replace(fullMatchedString, it)
                                pasteText(inputNode, newText)
                                showUndoButton()
                            }.onFailure {
                                showToast(it.message ?: "Unknown error")
                            }
                        }
                        return // Consume the event after processing one inline command
                    }
                }

                // --- Process Trailing Triggers (Old Logic) ---
                for (i in 0 until triggers.length()) {
                    val triggerObj = triggers.getJSONObject(i)
                    val pattern = triggerObj.getString("pattern")
                    val prompt = triggerObj.getString("prompt")

                    if (isTriggerValid(currentText, pattern)) {
                        val textToProcess = currentText.substring(0, currentText.length - pattern.length).trim()
                        
                        if (textToProcess.length > 1) {
                            originalTextCache = textToProcess
                            lastNode = inputNode
                            undoCacheTimestamp = System.currentTimeMillis()
                            HistoryManager.add(originalTextCache) // Save to history

                            showLoading()
                            hideUndoButton() 

                            geminiApiClient.callGemini(apiKey, model, prompt, textToProcess, temp, topP) { result ->
                                hideLoading()
                                result.onSuccess {
                                    pasteText(inputNode, it)
                                    showUndoButton()
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

    private fun showLoading() {
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

    private fun showUndoButton() {
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

    override fun onInterrupt() { hideLoading(); hideUndoButton() }
}
