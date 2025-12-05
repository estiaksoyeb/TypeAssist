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
import android.widget.Toast
import com.typeassist.app.api.GeminiApiClient
import okhttp3.*
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
            val currentText = inputNode.text?.toString() ?: ""

            val prefs = getSharedPreferences("GeminiConfig", Context.MODE_PRIVATE)
            val configJson = prefs.getString("config_json", null) ?: return

            try {
                val configObj = JSONObject(configJson)
                
                if (configObj.has("isAppEnabled") && !configObj.getBoolean("isAppEnabled")) {
                    return
                }

                val apiKey = configObj.getString("apiKey").trim()
                val model = configObj.getString("model").trim()
                
                var temp = 0.2
                var topP = 0.95
                if (configObj.has("generationConfig")) {
                    val genConfig = configObj.getJSONObject("generationConfig")
                    temp = genConfig.optDouble("temperature", 0.2)
                    topP = genConfig.optDouble("topP", 0.95)
                }

                val triggers = configObj.getJSONArray("triggers")

                for (i in 0 until triggers.length()) {
                    val triggerObj = triggers.getJSONObject(i)
                    val pattern = triggerObj.getString("pattern")
                    val prompt = triggerObj.getString("prompt")

                    if (currentText.contains(pattern)) {
                        val textToProcess = currentText.replace(pattern, "").trim()
                        
                        if (textToProcess.length > 1) {
                            originalTextCache = currentText
                            lastNode = inputNode

                            showLoading()
                            hideUndoButton() 

                            geminiApiClient.callGemini(apiKey, model, prompt, textToProcess, temp, topP) { result ->
                                hideLoading()
                                result.onSuccess {
                                    pasteText(inputNode, it)
                                    showUndoButton()
                                }.onFailure {
                                    showToast("AI Error: ${it.message}")
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
        if (lastNode != null && originalTextCache.isNotEmpty()) {
            if (lastNode!!.refresh()) { pasteText(lastNode!!, originalTextCache); showToast("Undone!") }
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

    override fun onInterrupt() { hideLoading(); hideUndoButton() }
}
