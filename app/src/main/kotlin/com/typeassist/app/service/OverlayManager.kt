package com.typeassist.app.service

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.Toast
import com.typeassist.app.data.AppConfig

class OverlayManager(private val context: Context) {

    private var windowManager: WindowManager? = null
    
    // --- UI Elements ---
    private var loadingView: FrameLayout? = null
    private var undoView: FrameLayout? = null
    private var previewView: FrameLayout? = null 

    private val mainHandler = Handler(Looper.getMainLooper())
    private val hideUndoRunnable = Runnable { hideUndoButton() }
    private val hidePreviewRunnable = Runnable { hidePreviewDialog() }
    
    // Callback for Undo action
    var onUndoAction: (() -> Unit)? = null

    init {
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    fun showLoading(config: AppConfig) {
        if (!config.enableLoadingOverlay) return
        mainHandler.post {
            if (loadingView != null) return@post
            loadingView = FrameLayout(context).apply {
                setBackgroundColor(0x77000000.toInt())
                setPadding(30, 30, 30, 30)
                background = GradientDrawable().apply { setColor(0x99000000.toInt()); cornerRadius = 40f }
            }
            val progressBar = ProgressBar(context)
            progressBar.indeterminateTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
            loadingView?.addView(progressBar)
            val params = WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT)
            params.gravity = Gravity.CENTER
            try { windowManager?.addView(loadingView, params) } catch (e: Exception) {}
        }
    }

    fun hideLoading() {
        mainHandler.post {
            if (loadingView != null) { try { windowManager?.removeView(loadingView); loadingView = null } catch (e: Exception) {} }
        }
    }

    fun showUndoButton(config: AppConfig) {
        if (!config.enableUndoOverlay) return
        mainHandler.post {
            if (undoView != null) return@post
            undoView = FrameLayout(context)
            val btn = Button(context).apply {
                text = "UNDO"
                textSize = 14f
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply { setColor(0xEE333333.toInt()); cornerRadius = 50f; setStroke(2, Color.WHITE) }
                setOnClickListener { 
                    onUndoAction?.invoke() 
                    hideUndoButton()
                }
            }
            undoView?.addView(btn)
            val params = WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT)
            params.gravity = Gravity.CENTER
            try { windowManager?.addView(undoView, params); mainHandler.postDelayed(hideUndoRunnable, 5000) } catch (e: Exception) {}
        }
    }

    fun hideUndoButton() {
        mainHandler.removeCallbacks(hideUndoRunnable)
        mainHandler.post {
            if (undoView != null) { try { windowManager?.removeView(undoView); undoView = null } catch (e: Exception) {} }
        }
    }

    fun hidePreviewDialog() {
        mainHandler.post { removePreviewInternal() }
    }

    private fun removePreviewInternal() {
        mainHandler.removeCallbacks(hidePreviewRunnable)
        if (previewView != null) {
            try {
                windowManager?.removeView(previewView)
            } catch (e: Exception) {
            } finally {
                previewView = null
            }
        }
    }

    fun showPreviewDialog(text: String, isDarkMode: Boolean, onInsert: () -> Unit) {
        mainHandler.post {
            removePreviewInternal()
            
            // Material 3 Colors from Theme.kt
            val cardBgColor = if (isDarkMode) 0xFF1C1B1F.toInt() else 0xFFFFFBFE.toInt() // Surface
            val primaryTextColor = if (isDarkMode) 0xFF818CF8.toInt() else 0xFF4F46E5.toInt() // Primary
            val secondaryTextColor = if (isDarkMode) 0xFFE6E1E5.toInt() else 0xFF1C1B1F.toInt() // OnSurface
            val discardTextColor = if (isDarkMode) 0xFFCAC4D0.toInt() else 0xFF49454F.toInt() // OnSurfaceVariant
            val insertTextColor = if (isDarkMode) 0xFF818CF8.toInt() else 0xFF4F46E5.toInt() // Primary

            // The Card (As Root View)
            val card = android.widget.LinearLayout(context).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                setPadding(40, 40, 40, 40)
                background = GradientDrawable().apply { 
                    setColor(cardBgColor)
                    cornerRadius = 32f 
                    setStroke(3, insertTextColor) 
                }
                isClickable = true
                elevation = 20f
            }

            val title = android.widget.TextView(context).apply {
                this.text = "Preview Response"
                textSize = 18f
                setTextColor(primaryTextColor)
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(0, 0, 0, 20)
            }
            card.addView(title)

            val scrollView = android.widget.ScrollView(context).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 
                    0 
                ).apply { weight = 1f }
            }
            // Constrain height
            scrollView.layoutParams.height = (context.resources.displayMetrics.heightPixels * 0.35).toInt()
            
            val contentText = android.widget.TextView(context).apply {
                this.text = text
                textSize = 14f
                setTextColor(secondaryTextColor)
            }
            scrollView.addView(contentText)
            card.addView(scrollView)

            val btnRow = android.widget.LinearLayout(context).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                gravity = Gravity.END
                setPadding(0, 30, 0, 0)
            }

            fun createButton(label: String, color: Int, onClick: () -> Unit): Button {
                return Button(context).apply {
                    this.text = label
                    setTextColor(color)
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    background = android.util.TypedValue().let { tv ->
                        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, tv, true)
                        context.resources.getDrawable(tv.resourceId, context.theme)
                    }
                    setPadding(30, 20, 30, 20)
                    setOnClickListener { onClick() }
                }
            }

            val discardBtn = createButton("Discard", discardTextColor) { hidePreviewDialog() }
            val insertBtn = createButton("Insert", insertTextColor) { 
                onInsert()
                hidePreviewDialog() 
            }

            btnRow.addView(discardBtn)
            btnRow.addView(insertBtn)
            card.addView(btnRow)

            previewView = FrameLayout(context)
            previewView?.addView(card)

            val rootParams = WindowManager.LayoutParams(
                (context.resources.displayMetrics.widthPixels * 0.75).toInt(), 
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
            }
            
            try { 
                windowManager?.addView(previewView, rootParams) 
                mainHandler.postDelayed(hidePreviewRunnable, 30000)
            } catch (e: Exception) {}
        }
    }
    
    fun showToast(message: String) {
        mainHandler.post { Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
    }
    
    fun hideAll() {
        hideLoading()
        hideUndoButton()
        hidePreviewDialog()
    }
}