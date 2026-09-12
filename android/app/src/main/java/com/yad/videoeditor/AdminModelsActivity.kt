package com.yad.videoeditor

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AdminModelsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_models)

        val container = findViewById<LinearLayout>(R.id.modelsContainer)
        val btnSave = findViewById<Button>(R.id.btnSaveModels)

        val allModels = ModelPresets.ALL.map { it.id }
        val checkboxes = mutableMapOf<String, CheckBox>()

        // Create checkbox per model
        for (model in ModelPresets.ALL) {
            val cb = CheckBox(this).apply {
                text = model.displayName
                isChecked = true
                setPadding(16, 16, 16, 16)
            }
            checkboxes[model.id] = cb
            container?.addView(cb)
        }

        btnSave?.setOnClickListener {
            val enabled = checkboxes.filter { it.value.isChecked }.keys.toList()
            FirebaseManager.setEnabledModels(enabled) { ok ->
                Toast.makeText(this,
                    if (ok) "✅ Tersimpan (${enabled.size} model)" else "❌ Gagal",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }
}
