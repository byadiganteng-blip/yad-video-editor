package com.yad.videoeditor

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * AutoLogSaver — otomatis simpan semua error ke file.
 *
 * Cara pakai:
 *   // Di Application.onCreate()
 *   AutoLogSaver.init(this)
 *
 *   // Di mana saja untuk log manual
 *   AutoLogSaver.log("TAG", "Pesan")
 *   AutoLogSaver.logError("TAG", "Pesan", exception)
 *
 * File log tersimpan di:
 *   /storage/emulated/0/YADVideoEditor/logs/error.log
 *
 * Cara akses:
 *   1. Buka File Manager
 *   2. Cari folder "YADVideoEditor"
 *   3. Buka "logs" → "error.log"
 */
object AutoLogSaver {

    private const val TAG = "AutoLogSaver"
    private const val FOLDER_NAME = "YADVideoEditor"
    private const val LOG_FILE = "error.log"
    private const val MAX_LOG_SIZE = 5 * 1024 * 1024L  // 5 MB max

    private var logFile: File? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    /**
     * Init — panggil di Application.onCreate()
     */
    fun init(context: Context) {
        try {
            // Coba pakai external storage dulu (mudah diakses)
            val externalDir = File(
                Environment.getExternalStorageDirectory(),
                FOLDER_NAME
            )

            // Cek apakah bisa tulis ke external
            val dir = if (canWriteTo(externalDir)) {
                File(externalDir, "logs")
            } else {
                // Fallback: pakai app-specific dir
                File(context.getExternalFilesDir(null), "logs")
            }

            if (!dir.exists()) {
                dir.mkdirs()
            }

            logFile = File(dir, LOG_FILE)

            // Rotate kalau file terlalu besar
            if (logFile!!.exists() && logFile!!.length() > MAX_LOG_SIZE) {
                val backup = File(dir, "error_old.log")
                if (backup.exists()) backup.delete()
                logFile!!.renameTo(backup)
            }

            // Tulis header
            writeRaw("\n\n========== SESSION START ==========")
            writeRaw("Waktu: ${dateFormat.format(Date())}")
            writeRaw("App: YAD Video Editor")
            writeRaw("===================================\n")

            // Pasang global handler
            setupGlobalExceptionHandler(context)

            Log.d(TAG, "AutoLogSaver initialized: ${logFile!!.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Init failed: ${e.message}", e)
        }
    }

    /**
     * Cek apakah folder bisa ditulis
     */
    private fun canWriteTo(dir: File): Boolean {
        return try {
            if (!dir.exists()) dir.mkdirs()
            val test = File(dir, ".test")
            test.writeText("test")
            val ok = test.exists()
            test.delete()
            ok
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Log pesan biasa
     */
    fun log(tag: String, message: String) {
        val line = "[${dateFormat.format(Date())}] [INFO] [$tag] $message"
        Log.d(tag, message)
        writeRaw(line)
    }

    /**
     * Log warning
     */
    fun warn(tag: String, message: String) {
        val line = "[${dateFormat.format(Date())}] [WARN] [$tag] $message"
        Log.w(tag, message)
        writeRaw(line)
    }

    /**
     * Log error dengan exception + stack trace
     */
    fun logError(tag: String, message: String, e: Throwable? = null) {
        val sb = StringBuilder()
        sb.append("[${dateFormat.format(Date())}] [ERROR] [$tag] $message")
        if (e != null) {
            sb.append("\n  Exception: ${e.javaClass.name}")
            sb.append("\n  Message: ${e.message}")
            val sw = StringWriter()
            e.printStackTrace(PrintWriter(sw))
            sb.append("\n  Stack trace:")
            sb.append("\n")
            sb.append(sw.toString())
        }
        val line = sb.toString()
        Log.e(tag, message, e)
        writeRaw(line)
    }

    /**
     * Tulis langsung ke file
     */
    private fun writeRaw(text: String) {
        try {
            val f = logFile ?: return
            f.appendText(text + "\n")
        } catch (e: Exception) {
            Log.e(TAG, "writeRaw failed: ${e.message}")
        }
    }

    /**
     * Pasang global exception handler — menangkap semua crash
     */
    private fun setupGlobalExceptionHandler(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                logError("CRASH", "Uncaught exception di thread ${thread.name}", throwable)
                writeRaw("========== CRASH ==========")
            } catch (_: Exception) {}
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    /**
     * Ambil path file log (untuk share/ditampilkan)
     */
    fun getLogPath(): String? = logFile?.absolutePath

    /**
     * Ambil isi log
     */
    fun getLogContent(): String? {
        return try {
            logFile?.readText()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Hapus log
     */
    fun clearLog() {
        try {
            logFile?.writeText("")
        } catch (_: Exception) {}
    }
}
