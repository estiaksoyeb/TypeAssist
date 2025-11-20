package com.typeassist.app

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
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class MyAccessibilityService : AccessibilityService() {

    private val client = OkHttpClient()
    private var windowManager: WindowManager? = null
    
    // --- UI Elements ---
    private var loadingView: FrameLayout? = null
    private var undoView: FrameLayout? = null
    private var isLoaderVisible = false
    
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

        // 1. PREVENT SELF-TRIGGERING
        // If typing inside TypeAssist app, ignore it.
        if (event.packageName?.toString() == packageName) return

        if (event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            val inputNode = event.source ?: return
            val currentText = inputNode.text?.toString() ?: ""

            // Load Config
            val prefs = getSharedPreferences("GeminiConfig", Context.MODE_PRIVATE)
            val configJson = prefs.getString("config_json", null) ?: return

            try {
                val configObj = JSONObject(configJson)
                
                // 2. CHECK MASTER SWITCH
                // If the switch in the App UI is OFF, stop here.
                if (configObj.has("isAppEnabled") && !configObj.getBoolean("isAppEnabled")) {
                    return
                }

                val apiKey = configObj.getString("apiKey").trim()
                val model = configObj.getString("model").trim()
                
                // Get Generation Settings
                var temp = 0.2
                var topP = 0.95
                if (configObj.has("generationConfig")) {
                    val genConfig = configObj.getJSONObject("generationConfig")
                    temp = genConfig.optDouble("temperature", 0.2)
                    topP = genConfig.optDouble("topP", 0.95)
                }

                val triggers = configObj.getJSONArray("triggers")

                // Check Triggers
                for (i in 0 until triggers.length()) {
                    val triggerObj = triggers.getJSONObject(i)
                    val pattern = triggerObj.getString("pattern")
                    val prompt = triggerObj.getString("prompt")

                    if (currentText.contains(pattern)) {
                        val textToProcess = currentText.replace(pattern, "").trim()
                        
                        if (textToProcess.length > 1) {
                            // Save for Undo
                            originalTextCache = currentText
                            lastNode = inputNode

                            // Show Visuals
                            showLoading()
                            hideUndoButton() // Hide old undo if visible

                            // Call API
                            callGemini(inputNode, apiKey, model, prompt, textToProcess, temp, topP)
                        }
                        return 
                    }
                }
            } catch (e: Exception) {
                // Silent fail (don't crash while typing)
            }
        }
    }

    // ----------------------------------------------------------
    // API CALL
    // ----------------------------------------------------------

    private fun callGemini(node: AccessibilityNodeInfo, apiKey: String, model: String, prompt: String, userText: String, temp: Double, topP: Double) {
        val jsonBody = JSONObject()
        val contentsArray = JSONArray()
        val contentObject = JSONObject()
        val partsArray = JSONArray()
        val partObject = JSONObject()
        
        partObject.put("text", "$prompt\n\nInput: $userText")
        partsArray.put(partObject)
        contentObject.put("parts", partsArray)
        contentsArray.put(contentObject)
        jsonBody.put("contents", contentsArray)

        val genConfig = JSONObject()
        genConfig.put("temperature", temp)
        genConfig.put("topP", topP)
        jsonBody.put("generationConfig", genConfig)

        val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        val request = Request.Builder().url(url).post(requestBody).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                hideLoading()
                showToast("Network Error")
            }

            override fun onResponse(call: Call, response: Response) {
                hideLoading()
                response.use {
                    if (!it.isSuccessful) { showToast("Error: ${it.code}"); return }
                    try {
                        val responseData = it.body?.string()
                        val jsonResponse = JSONObject(responseData)
                        val resultText = jsonResponse.getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text")
                        
                        // Paste and Show Undo
                        pasteText(node, resultText.trim())
                        showUndoButton()
                        
                    } catch (e: Exception) { showToast("AI parsing error") }
                }
            }
        })
    }

    // ----------------------------------------------------------
    // UI HELPERS (Loading & Undo)
    // ----------------------------------------------------------

    private fun showLoading() {
        Handler(Looper.getMainLooper()).post {
            if (loadingView != null) return@post

            loadingView = FrameLayout(this).apply {
                setBackgroundColor(0x77000000.toInt())
                setPadding(30, 30, 30, 30)
                background = GradientDrawable().apply {
                    setColor(0x99000000.toInt())
                    cornerRadius = 40f
                }
            }

            val progressBar = ProgressBar(this)
            progressBar.indeterminateTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
            loadingView?.addView(progressBar)

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )
            params.gravity = Gravity.CENTER

            try {
                windowManager?.addView(loadingView, params)
                isLoaderVisible = true
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun hideLoading() {
        Handler(Looper.getMainLooper()).post {
            if (loadingView != null) {
                try {
                    windowManager?.removeView(loadingView)
                    loadingView = null
                    isLoaderVisible = false
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
    }

    private fun showUndoButton() {
        Handler(Looper.getMainLooper()).post {
            if (undoView != null) return@post

            undoView = FrameLayout(this)
            val btn = Button(this).apply {
                text = "UNDO CHANGE"
                textSize = 14f
                setTextColor(Color.WHITE)
                isAllCaps = true
                setPadding(40, 20, 40, 20)
                
                background = GradientDrawable().apply {
                    setColor(0xEE333333.toInt()) 
                    cornerRadius = 50f
                    setStroke(2, Color.WHITE)
                }
                
                setOnClickListener { performUndo() }
            }
            undoView?.addView(btn)

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )
            // CENTER GRAVITY (As requested)
            params.gravity = Gravity.CENTER
            params.y = 0 

            try {
                windowManager?.addView(undoView, params)
                undoHandler.postDelayed(hideUndoRunnable, 5000)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun hideUndoButton() {
        undoHandler.removeCallbacks(hideUndoRunnable)
        Handler(Looper.getMainLooper()).post {
            if (undoView != null) {
                try {
                    windowManager?.removeView(undoView)
                    undoView = null
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
    }

    private fun performUndo() {
        if (lastNode != null && originalTextCache.isNotEmpty()) {
            if (lastNode!!.refresh()) {
                pasteText(lastNode!!, originalTextCache)
                Toast.makeText(this, "Restored!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Cannot Undo (Field lost)", Toast.LENGTH_SHORT).show()
            }
        }
        hideUndoButton()
    }

    // ----------------------------------------------------------
    // UTILS
    // ----------------------------------------------------------

    private fun pasteText(node: AccessibilityNodeInfo, text: String) {
        val arguments = Bundle()
        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        node.refresh()
        node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post { Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show() }
    }

    override fun onInterrupt() {
        hideLoading()
        hideUndoButton()
    }
}
