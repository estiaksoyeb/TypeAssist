package com.typeassist.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.regex.Pattern
import com.typeassist.app.api.GeminiApiClient
import com.typeassist.app.api.CloudflareApiClient
import com.typeassist.app.api.CustomApiClient
import com.typeassist.app.data.HistoryManager
import com.typeassist.app.data.AppConfig
import com.google.gson.Gson
import okhttp3.*

class MyAccessibilityService : AccessibilityService() {

    private val client = OkHttpClient()
    private val geminiApiClient = GeminiApiClient(client)
    private val cloudflareApiClient = CloudflareApiClient(client)
    private val customApiClient = CustomApiClient(client)
    
    private lateinit var overlayManager: OverlayManager
    
    // -- Undo Cache --
    private var lastNode: AccessibilityNodeInfo? = null
    private var originalTextCache: String = ""
    private var undoCacheTimestamp: Long = 0L

    // -- Debounce --
    private val debounceHandler = Handler(Looper.getMainLooper())
    private var pendingTriggerRunnable: Runnable? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        overlayManager = OverlayManager(this)
        overlayManager.onUndoAction = { performUndo() }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        if (event.packageName?.toString() == packageName) {
            val prefs = getSharedPreferences("GeminiConfig", Context.MODE_PRIVATE)
            val isTesting = prefs.getBoolean("is_testing_active", false)
            if (!isTesting) return
        }

        if (event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            debounceHandler.removeCallbacksAndMessages(null)

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

                // --- 0. Global Inline Transformation ---
                val globalTransformRegex = Pattern.compile("""(?s)(.*)\.\.\.(.+?)\.\.\.$""")
                val globalMatcher = globalTransformRegex.matcher(currentText)
                if (globalMatcher.find()) {
                    val contextText = globalMatcher.group(1) ?: ""
                    val instruction = globalMatcher.group(2) ?: ""

                    if (contextText.isNotBlank() && instruction.isNotBlank()) {
                        originalTextCache = currentText
                        HistoryManager.add(originalTextCache)
                        lastNode = inputNode
                        undoCacheTimestamp = System.currentTimeMillis()

                        overlayManager.showLoading(config)
                        overlayManager.hideUndoButton()

                        val systemPrompt = "Rewrite the following text according to this instruction: $instruction. Return ONLY the rewritten text, no explanations, no chat."
                        
                        performAICall(config, systemPrompt, contextText) { result ->
                            overlayManager.hideLoading()
                            result.onSuccess { aiText ->
                                processAiResult(config, inputNode, currentText, aiText, replaceWhole = true)
                            }.onFailure {
                                overlayManager.showToast(it.message ?: "Unknown error")
                            }
                        }
                        return
                    }
                }

                // -- Snippets Logic --
                val snippetPrefix = config.snippetTriggerPrefix
                val saveSnippetPattern = config.saveSnippetPattern
                val snippets = config.snippets

                for (s in snippets) {
                    val fullTrigger = snippetPrefix + s.trigger
                    val idx = currentText.lastIndexOf(fullTrigger)
                    if (idx != -1) {
                        val isAtEnd = idx + fullTrigger.length == currentText.length
                        if (config.allowTriggerAnywhere || isAtEnd) {
                            val prefix = currentText.substring(0, idx)
                            val suffix = currentText.substring(idx + fullTrigger.length)
                            val newText = prefix + s.content + suffix
                            pasteText(inputNode, newText)
                            return
                        }
                    }
                }

                val saveMatcher = Pattern.compile(buildSaveSnippetRegex(saveSnippetPattern)).matcher(currentText)
                if (saveMatcher.find()) {
                    val fullMatch = saveMatcher.group(0) ?: ""
                    val newTrigger = saveMatcher.group(1)?.trim() ?: ""
                    val newContent = saveMatcher.group(2)?.trim() ?: ""

                    if (newTrigger.isNotEmpty() && newContent.isNotEmpty()) {
                        config.snippets.removeIf { it.trigger == newTrigger }
                        config.snippets.add(com.typeassist.app.data.Snippet(newTrigger, newContent))
                        prefs.edit().putString("config_json", gson.toJson(config)).apply()
                        val cleanText = currentText.replace(fullMatch, newContent)
                        pasteText(inputNode, cleanText)
                        overlayManager.showToast("Snippet '$newTrigger' saved!")
                        return
                    }
                }

                // -- Utility Belt --
                findBalancedCommand(currentText, "(.c:")?.let { (fullMatch, expr) ->
                    val result = com.typeassist.app.utils.UtilityBelt.evaluateMath(expr)
                    originalTextCache = currentText
                    HistoryManager.add(originalTextCache)
                    lastNode = inputNode
                    undoCacheTimestamp = System.currentTimeMillis()
                    
                    // Safe replacement using lastIndexOf to ensure we replace the found instance
                    val idx = currentText.lastIndexOf(fullMatch)
                    if (idx != -1) {
                        val prefix = currentText.substring(0, idx)
                        val suffix = currentText.substring(idx + fullMatch.length)
                        val newText = prefix + result + suffix
                        pasteText(inputNode, newText)
                    }
                    overlayManager.showUndoButton(config)
                    return
                }

                val utilityTriggers = mapOf(
                    ".now" to { com.typeassist.app.utils.UtilityBelt.getTime() },
                    ".date" to { com.typeassist.app.utils.UtilityBelt.getDate() },
                    ".pass" to { com.typeassist.app.utils.UtilityBelt.generatePassword() }
                )

                for ((uTrigger, uAction) in utilityTriggers) {
                    val idx = currentText.lastIndexOf(uTrigger)
                    if (idx != -1) {
                         val isAtEnd = idx + uTrigger.length == currentText.length
                         if (config.allowTriggerAnywhere || isAtEnd) {
                             val result = uAction()
                             originalTextCache = currentText
                             HistoryManager.add(originalTextCache)
                             lastNode = inputNode
                             undoCacheTimestamp = System.currentTimeMillis()
                             
                             val prefix = currentText.substring(0, idx)
                             val suffix = currentText.substring(idx + uTrigger.length)
                             val newText = prefix + result + suffix
                             
                             pasteText(inputNode, newText)
                             overlayManager.showUndoButton(config)
                             return
                         }
                    }
                }

                val undoCommandPattern = config.undoCommandPattern.trim()
                val timeSinceCache = System.currentTimeMillis() - undoCacheTimestamp
                if (currentText.endsWith(undoCommandPattern) && originalTextCache.isNotEmpty() && timeSinceCache < 300000) {
                    pasteText(inputNode, originalTextCache)
                    return
                }
                
                val triggers = config.triggers
                val inlineCommands = config.inlineCommands

                // -- Process Inline Commands --
                for (inlineCommand in inlineCommands) {
                    val inlinePattern = inlineCommand.pattern
                    val inlinePromptTemplate = inlineCommand.prompt
                    val regexPattern = Pattern.compile(buildRegexFromInlinePattern(inlinePattern))
                    val matcher = regexPattern.matcher(currentText)

                    if (matcher.find()) {
                        val fullMatchedString = matcher.group(0) ?: continue
                        val userPrompt = matcher.group(1) ?: continue

                        originalTextCache = currentText
                        HistoryManager.add(originalTextCache)
                        lastNode = inputNode

                        overlayManager.showLoading(config)
                        overlayManager.hideUndoButton()

                        performAICall(config, inlinePromptTemplate, userPrompt) { result ->
                            overlayManager.hideLoading()
                            result.onSuccess { aiText ->
                                val wordCount = aiText.split("\\s+".toRegex()).size
                                val nightModeFlags = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
                                val isDarkMode = nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES

                                if (wordCount > 15 && config.enablePreviewDialog) {
                                    overlayManager.showPreviewDialog(aiText, isDarkMode) {
                                        val newText = currentText.replace(fullMatchedString, aiText)
                                        pasteText(inputNode, newText)
                                        overlayManager.showUndoButton(config)
                                    }
                                } else {
                                    val newText = currentText.replace(fullMatchedString, aiText)
                                    pasteText(inputNode, newText)
                                    overlayManager.showUndoButton(config)
                                }
                            }.onFailure {
                                overlayManager.showToast(it.message ?: "Unknown error")
                            }
                        }
                        return
                    }
                }

                // -- Process Trailing Triggers (Debounced) --
                for (trigger in triggers) {
                    val pattern = trigger.pattern
                    val prompt = trigger.prompt

                    val triggerIndex = findTriggerIndex(currentText, pattern, config.allowTriggerAnywhere)
                    if (triggerIndex != -1) {
                        val textToProcess = currentText.substring(0, triggerIndex).trim()
                        val suffix = if (currentText.length > triggerIndex + pattern.length) {
                             currentText.substring(triggerIndex + pattern.length)
                        } else { "" }
                        
                        if (textToProcess.length > 1) {
                            val runnable = Runnable {
                                if (!inputNode.refresh()) return@Runnable
                                originalTextCache = textToProcess
                                lastNode = inputNode
                                undoCacheTimestamp = System.currentTimeMillis()
                                HistoryManager.add(originalTextCache)

                                overlayManager.showLoading(config)
                                overlayManager.hideUndoButton() 

                                performAICall(config, prompt, textToProcess) { result ->
                                    overlayManager.hideLoading()
                                    result.onSuccess { aiText ->
                                        val finalText = aiText + suffix
                                        processAiResult(config, inputNode, null, finalText, replaceWhole = true)
                                    }.onFailure {
                                        overlayManager.showToast(it.message ?: "Unknown error")
                                    }
                                }
                            }
                            pendingTriggerRunnable = runnable
                            debounceHandler.postDelayed(runnable, config.triggerDebounceMs)
                        }
                        return
                    }
                }
            } catch (e: Exception) {}
        }
    }

    private fun processAiResult(config: AppConfig, node: AccessibilityNodeInfo, currentText: String?, aiText: String, replaceWhole: Boolean) {
        val wordCount = aiText.split("\\s+".toRegex()).size
        val nightModeFlags = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        val isDarkMode = nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES

        if (wordCount > 15 && config.enablePreviewDialog) {
            overlayManager.showPreviewDialog(aiText, isDarkMode) {
                if (replaceWhole || currentText == null) {
                    pasteText(node, aiText)
                } 
                overlayManager.showUndoButton(config)
            }
        } else {
             if (replaceWhole || currentText == null) {
                pasteText(node, aiText)
            }
            overlayManager.showUndoButton(config)
        }
    }

    private fun performAICall(config: AppConfig, prompt: String, userText: String, callback: (Result<String>) -> Unit) {
        if (config.provider == "cloudflare") {
            cloudflareApiClient.callCloudflare(config.cloudflareConfig.accountId, config.cloudflareConfig.apiToken, config.cloudflareConfig.model, prompt, userText, callback)
        } else if (config.provider == "custom") {
            customApiClient.callCustomApi(config.customApiConfig.baseUrl, config.customApiConfig.apiKey, config.customApiConfig.model, prompt, userText, callback)
        } else {
            geminiApiClient.callGemini(config.apiKey, config.model, prompt, userText, config.generationConfig.temperature, config.generationConfig.topP, callback)
        }
    }

    private fun findTriggerIndex(text: String, trigger: String, allowAnywhere: Boolean): Int {
        if (!allowAnywhere) {
            if (!text.endsWith(trigger)) return -1
            val triggerStartIndex = text.length - trigger.length
            if (triggerStartIndex > 0 && !text[triggerStartIndex - 1].isWhitespace()) return -1
            return triggerStartIndex
        }

        var idx = text.lastIndexOf(trigger)
        while (idx != -1) {
            val startOk = (idx == 0) || text[idx - 1].isWhitespace()
            val endIdx = idx + trigger.length
            val endOk = (endIdx == text.length) || text[endIdx].isWhitespace()
            
            if (startOk && endOk) return idx
            
            idx = text.lastIndexOf(trigger, idx - 1)
        }
        return -1
    }

    private fun performUndo() {
        val timeSinceCache = System.currentTimeMillis() - undoCacheTimestamp
        if (lastNode != null && originalTextCache.isNotEmpty() && timeSinceCache < 300000) {
            if (lastNode!!.refresh()) {
                pasteText(lastNode!!, originalTextCache)
                overlayManager.showToast("Undone!")
            }
        }
        overlayManager.hideUndoButton()
    }

    private fun pasteText(node: AccessibilityNodeInfo, text: String) {
        val arguments = Bundle()
        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        node.refresh()
        node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }

    private fun buildRegexFromInlinePattern(inlinePattern: String): String {
        return Pattern.quote(inlinePattern).replace("%", "\\E(.+?)\\Q").replace("\\Q\\E", "")
    }

    private fun buildSaveSnippetRegex(pattern: String): String {
        val parts = pattern.split("%", limit = 3)
        if (parts.size != 3) return Pattern.quote(pattern)
        return Pattern.quote(parts[0]) + "(.+?)" + Pattern.quote(parts[1]) + "(.+?)" + Pattern.quote(parts[2])
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
                ')' -> { if (balance == 0) { endIndex = i; break }; balance-- }
            }
        }
        if (endIndex != -1) return Pair(text.substring(startIndex, endIndex + 1), text.substring(contentStartIndex, endIndex))
        return null
    }

    override fun onInterrupt() { overlayManager.hideAll() }
}
