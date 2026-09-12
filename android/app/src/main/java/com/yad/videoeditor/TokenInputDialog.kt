package com.yad.videoeditor

import android.app.AlertDialog
import android.content.Context
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Dialog untuk minta token GitHub dari user.
 * Token disimpan di SharedPreferences.
 */
object TokenInputDialog {

    fun show(context: Context, onSaved: (Boolean) -> Unit = {}) {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 30)
        }

        val tvInfo = TextView(context).apply {
            text = "Masukkan GitHub Personal Access Token (PAT)\n\n" +
                   "Cara dapat:\n" +
                   "1. Buka github.com/settings/tokens\n" +
                   "2. Generate new token (classic)\n" +
                   "3. Centang: repo + workflow\n" +
                   "4. Copy token (diawali 'ghp_')\n\n" +
                   "Token disimpan lokal di HP Anda."
            textSize = 12f
        }

        val etToken = EditText(context).apply {
            hint = "ghp_xxxxxxxxxxxxxxxx"
            inputType = InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        container.addView(tvInfo)
        container.addView(etToken)

        AlertDialog.Builder(context)
            .setTitle("🔑 GitHub Token")
            .setView(container)
            .setCancelable(false)
            .setPositiveButton("Simpan") { _, _ ->
                val t = etToken.text.toString().trim()
                if (t.startsWith("ghp_") && t.length > 20) {
                    SecureConfig.setGithubToken(t)
                    onSaved(true)
                } else {
                    onSaved(false)
                }
            }
            .setNegativeButton("Nanti", null)
            .show()
    }
}
