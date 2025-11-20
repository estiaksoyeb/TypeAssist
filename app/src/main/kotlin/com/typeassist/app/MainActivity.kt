package com.typeassist.app

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.*
import com.google.gson.GsonBuilder
import java.io.Serializable
import okhttp3.*
import java.io.IOException

// --- DATA MODELS ---
data class AppConfig(
    var isAppEnabled: Boolean = true,
    var apiKey: String = "",
    var model: String = "gemini-2.5-flash",
    var generationConfig: GenConfig = GenConfig(),
    var triggers: MutableList<Trigger> = mutableListOf()
) : Serializable

data class GenConfig(
    var temperature: Double = 0.2,
    var topP: Double = 0.95
) : Serializable

data class Trigger(
    var pattern: String,
    var prompt: String
) : Serializable

// NOTE: Extending Activity (not AppCompatActivity) to prevent Theme crashes
class MainActivity : Activity() {

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private var currentConfig = AppConfig()
    private lateinit var listContainer: LinearLayout
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- CRASH CATCHER SYSTEM ---
        try {
            setContentView(R.layout.activity_main)
            initUI() // If this fails, it jumps to 'catch'
        } catch (e: Exception) {
            // If XML fails, build a simple error screen programmatically
            val scroll = ScrollView(this)
            val text = TextView(this).apply {
                text = "CRASH CAUGHT!\n\nError: ${e.message}\n\n${e.stackTraceToString()}"
                setTextColor(Color.RED)
                textSize = 16f
                setPadding(50, 50, 50, 50)
            }
            scroll.addView(text)
            setContentView(scroll)
        }
    }

    private fun initUI() {
        listContainer = findViewById(R.id.listCommandsInternal)
        loadConfig()

        // Toggle Logic
        val switchActive = findViewById<Switch>(R.id.switchServiceActive)
        switchActive.isChecked = currentConfig.isAppEnabled
        switchActive.setOnCheckedChangeListener { _, isChecked ->
            currentConfig.isAppEnabled = isChecked
            saveConfig()
        }

        findViewById<Button>(R.id.btnSysPermission).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        // Page Navigation
        val pages = listOf(
            findViewById<View>(R.id.pageHome),
            findViewById<View>(R.id.pageCommands),
            findViewById<View>(R.id.pageSettings),
            findViewById<View>(R.id.pageJson),
            findViewById<View>(R.id.pageTest)
        )

        fun showPage(pageId: Int) {
            pages.forEach { it.visibility = View.GONE }
            findViewById<View>(pageId).visibility = View.VISIBLE
        }

        // Nav Buttons
        findViewById<Button>(R.id.navCommands).setOnClickListener { refreshCommandList(); showPage(R.id.pageCommands) }
        findViewById<Button>(R.id.navSettings).setOnClickListener { loadSettingsToUI(); showPage(R.id.pageSettings) }
        findViewById<Button>(R.id.navJson).setOnClickListener { findViewById<EditText>(R.id.etJsonRaw).setText(gson.toJson(currentConfig)); showPage(R.id.pageJson) }
        findViewById<Button>(R.id.navTest).setOnClickListener { showPage(R.id.pageTest) }

        // Back Buttons
        val goHome = View.OnClickListener { showPage(R.id.pageHome) }
        findViewById<Button>(R.id.btnBackFromCmd).setOnClickListener(goHome)
        findViewById<Button>(R.id.btnBackFromSettings).setOnClickListener(goHome)
        findViewById<Button>(R.id.btnBackFromJson).setOnClickListener(goHome)
        findViewById<Button>(R.id.btnBackFromTest).setOnClickListener(goHome)

        // Settings Logic
        findViewById<Button>(R.id.btnSaveSettings).setOnClickListener {
            val newKey = findViewById<EditText>(R.id.etApiKey).text.toString().trim()
            currentConfig.apiKey = newKey
            currentConfig.model = findViewById<EditText>(R.id.etModel).text.toString().trim()
            
            try {
                val t = findViewById<EditText>(R.id.etTemp).text.toString()
                val p = findViewById<EditText>(R.id.etTopP).text.toString()
                if(t.isNotEmpty()) currentConfig.generationConfig.temperature = t.toDouble()
                if(p.isNotEmpty()) currentConfig.generationConfig.topP = p.toDouble()
            } catch(e:Exception){}
            
            saveConfig()
            Toast.makeText(this, "Saved. Verifying...", Toast.LENGTH_SHORT).show()
            if (newKey.isNotEmpty()) verifyApiConnection(newKey)
            goHome.onClick(it)
        }

        // Command Logic
        val etTrigger = findViewById<EditText>(R.id.etTrigger)
        val etPrompt = findViewById<EditText>(R.id.etPrompt)
        findViewById<Button>(R.id.btnSaveCmd).setOnClickListener {
            val trig = etTrigger.text.toString().trim()
            val prom = etPrompt.text.toString().trim()
            if (trig.isNotEmpty() && prom.isNotEmpty()) {
                currentConfig.triggers.removeIf { it.pattern == trig }
                currentConfig.triggers.add(Trigger(trig, prom))
                saveConfig()
                refreshCommandList()
                etTrigger.setText("")
                etPrompt.setText("")
                Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
            }
        }
        
        findViewById<Button>(R.id.btnDeleteCmd).setOnClickListener {
             val trig = etTrigger.text.toString().trim()
             if (currentConfig.triggers.removeIf { it.pattern == trig }) {
                 saveConfig()
                 refreshCommandList()
                 etTrigger.setText(""); etPrompt.setText("")
                 Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
             }
        }

        // JSON Logic
        findViewById<Button>(R.id.btnSaveJson).setOnClickListener {
            try {
                val raw = findViewById<EditText>(R.id.etJsonRaw).text.toString()
                currentConfig = gson.fromJson(raw, AppConfig::class.java)
                saveConfig()
                Toast.makeText(this, "Applied", Toast.LENGTH_SHORT).show()
                goHome.onClick(it)
            } catch(e:Exception) { Toast.makeText(this, "Invalid JSON", Toast.LENGTH_SHORT).show() }
        }
        
        findViewById<Button>(R.id.btnCopyJson).setOnClickListener {
            val cb = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Config", findViewById<EditText>(R.id.etJsonRaw).text.toString())
            cb.setPrimaryClip(clip)
            Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun verifyApiConnection(apiKey: String) {
        val request = Request.Builder().url("https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey").build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { Toast.makeText(this@MainActivity, "Network Error", Toast.LENGTH_SHORT).show() }
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) runOnUiThread { Toast.makeText(this@MainActivity, "API Verified! ✅", Toast.LENGTH_SHORT).show() }
                    else runOnUiThread { Toast.makeText(this@MainActivity, "Invalid API Key ❌", Toast.LENGTH_SHORT).show() }
                }
            }
        })
    }

    private fun loadConfig() {
        try {
            val prefs = getSharedPreferences("GeminiConfig", Context.MODE_PRIVATE)
            val json = prefs.getString("config_json", null)
            if (json == null) resetToDefaults()
            else {
                currentConfig = gson.fromJson(json, AppConfig::class.java)
                if(currentConfig.triggers == null) currentConfig.triggers = mutableListOf()
                if(currentConfig.generationConfig == null) currentConfig.generationConfig = GenConfig()
            }
        } catch (e: Exception) { resetToDefaults() }
    }

    private fun resetToDefaults() {
        currentConfig = AppConfig()
        currentConfig.triggers.add(Trigger("@fix", "Fix grammar:"))
        saveConfig()
    }

    private fun saveConfig() {
        getSharedPreferences("GeminiConfig", Context.MODE_PRIVATE).edit().putString("config_json", gson.toJson(currentConfig)).apply()
    }

    private fun refreshCommandList() {
        listContainer.removeAllViews()
        val etTrigger = findViewById<EditText>(R.id.etTrigger)
        val etPrompt = findViewById<EditText>(R.id.etPrompt)
        for (trigger in currentConfig.triggers) {
            val btn = Button(this)
            btn.text = trigger.pattern
            btn.setOnClickListener { etTrigger.setText(trigger.pattern); etPrompt.setText(trigger.prompt) }
            listContainer.addView(btn)
        }
    }
    
    private fun loadSettingsToUI() {
        findViewById<EditText>(R.id.etApiKey).setText(currentConfig.apiKey)
        findViewById<EditText>(R.id.etModel).setText(currentConfig.model)
        findViewById<EditText>(R.id.etTemp).setText(currentConfig.generationConfig.temperature.toString())
        findViewById<EditText>(R.id.etTopP).setText(currentConfig.generationConfig.topP.toString())
    }
}
