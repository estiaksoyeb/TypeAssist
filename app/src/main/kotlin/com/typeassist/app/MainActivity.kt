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

// Data Models
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

class MainActivity : Activity() {

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private var currentConfig = AppConfig()
    private lateinit var listContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize
        listContainer = findViewById(R.id.listCommandsInternal)
        loadConfig()

        // --- HOME PAGE LOGIC ---
        val switchActive = findViewById<Switch>(R.id.switchServiceActive)
        switchActive.isChecked = currentConfig.isAppEnabled
        switchActive.setOnCheckedChangeListener { _, isChecked ->
            currentConfig.isAppEnabled = isChecked
            saveConfig()
        }

        findViewById<Button>(R.id.btnSysPermission).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        // --- NAVIGATION LOGIC ---
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

        // --- COMMANDS PAGE LOGIC ---
        val etTrigger = findViewById<EditText>(R.id.etTrigger)
        val etPrompt = findViewById<EditText>(R.id.etPrompt)

        findViewById<Button>(R.id.btnSaveCmd).setOnClickListener {
            val trig = etTrigger.text.toString().trim()
            val prom = etPrompt.text.toString().trim()
            if (trig.isNotEmpty() && prom.isNotEmpty()) {
                // Remove existing if updating
                currentConfig.triggers.removeIf { it.pattern == trig }
                currentConfig.triggers.add(Trigger(trig, prom))
                saveConfig()
                refreshCommandList()
                Toast.makeText(this, "Command Saved", Toast.LENGTH_SHORT).show()
                etTrigger.setText("")
                etPrompt.setText("")
            }
        }
        
        findViewById<Button>(R.id.btnDeleteCmd).setOnClickListener {
             val trig = etTrigger.text.toString().trim()
             if (currentConfig.triggers.removeIf { it.pattern == trig }) {
                 saveConfig()
                 refreshCommandList()
                 etTrigger.setText("")
                 etPrompt.setText("")
                 Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
             }
        }

        // --- SETTINGS PAGE LOGIC ---
        findViewById<Button>(R.id.btnSaveSettings).setOnClickListener {
            currentConfig.apiKey = findViewById<EditText>(R.id.etApiKey).text.toString().trim()
            currentConfig.model = findViewById<EditText>(R.id.etModel).text.toString().trim()
            try {
                val t = findViewById<EditText>(R.id.etTemp).text.toString()
                val p = findViewById<EditText>(R.id.etTopP).text.toString()
                if(t.isNotEmpty()) currentConfig.generationConfig.temperature = t.toDouble()
                if(p.isNotEmpty()) currentConfig.generationConfig.topP = p.toDouble()
            } catch(e:Exception){}
            saveConfig()
            Toast.makeText(this, "Settings Saved", Toast.LENGTH_SHORT).show()
            goHome.onClick(it)
        }

        // --- JSON PAGE LOGIC ---
        findViewById<Button>(R.id.btnSaveJson).setOnClickListener {
            try {
                val raw = findViewById<EditText>(R.id.etJsonRaw).text.toString()
                currentConfig = gson.fromJson(raw, AppConfig::class.java)
                saveConfig()
                Toast.makeText(this, "JSON Applied", Toast.LENGTH_SHORT).show()
                goHome.onClick(it)
            } catch(e:Exception) { Toast.makeText(this, "Invalid JSON", Toast.LENGTH_SHORT).show() }
        }
        
        findViewById<Button>(R.id.btnCopyJson).setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Config", findViewById<EditText>(R.id.etJsonRaw).text.toString())
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Copied!", Toast.LENGTH_SHORT).show()
        }
    }

    // Helpers
    private fun loadConfig() {
        try {
            val prefs = getSharedPreferences("GeminiConfig", Context.MODE_PRIVATE)
            val json = prefs.getString("config_json", null)
            if (json == null) {
                resetToDefaults()
            } else {
                currentConfig = gson.fromJson(json, AppConfig::class.java)
                // Null safety
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
        val prefs = getSharedPreferences("GeminiConfig", Context.MODE_PRIVATE)
        prefs.edit().putString("config_json", gson.toJson(currentConfig)).apply()
    }

    private fun refreshCommandList() {
        listContainer.removeAllViews()
        val etTrigger = findViewById<EditText>(R.id.etTrigger)
        val etPrompt = findViewById<EditText>(R.id.etPrompt)
        
        for (trigger in currentConfig.triggers) {
            val btn = Button(this)
            btn.text = trigger.pattern
            // When clicked, load into edit boxes
            btn.setOnClickListener { 
                etTrigger.setText(trigger.pattern)
                etPrompt.setText(trigger.prompt)
            }
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            params.setMargins(0, 0, 0, 8)
            btn.layoutParams = params
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
