package com.yad.videoeditor

import android.content.Context

/**
 * Simpan state generate video agar tidak hilang saat keluar APK.
 * Data di-load ulang saat app dibuka.
 */
object GeneratorState {
    private const val PREF = "generator_state"

    private const val KEY_IS_RUNNING = "is_running"
    private const val KEY_PROGRESS = "progress"
    private const val KEY_MESSAGE = "message"
    private const val KEY_STARTED_AT = "started_at"
    private const val KEY_JOB_ID = "job_id"
    private const val KEY_RUN_ID = "run_id"
    private const val KEY_PROMPT = "prompt"
    private const val KEY_VOICE = "voice"
    private const val KEY_MODEL = "model"

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun saveRunning(ctx: Context, isRunning: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_IS_RUNNING, isRunning).apply()
    }

    fun isRunning(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_IS_RUNNING, false)

    fun saveProgress(ctx: Context, progress: Int, message: String) {
        prefs(ctx).edit()
            .putInt(KEY_PROGRESS, progress)
            .putString(KEY_MESSAGE, message)
            .apply()
    }

    fun getProgress(ctx: Context): Int =
        prefs(ctx).getInt(KEY_PROGRESS, 0)

    fun getMessage(ctx: Context): String =
        prefs(ctx).getString(KEY_MESSAGE, "") ?: ""

    fun saveJobInfo(ctx: Context, jobId: String, runId: Long,
                    prompt: String, voice: String, model: String) {
        prefs(ctx).edit()
            .putString(KEY_JOB_ID, jobId)
            .putLong(KEY_RUN_ID, runId)
            .putString(KEY_PROMPT, prompt)
            .putString(KEY_VOICE, voice)
            .putString(KEY_MODEL, model)
            .putLong(KEY_STARTED_AT, System.currentTimeMillis())
            .apply()
    }

    fun getJobId(ctx: Context): String =
        prefs(ctx).getString(KEY_JOB_ID, "") ?: ""

    fun getRunId(ctx: Context): Long =
        prefs(ctx).getLong(KEY_RUN_ID, 0L)

    fun getStartedAt(ctx: Context): Long =
        prefs(ctx).getLong(KEY_STARTED_AT, 0L)

    fun clear(ctx: Context) {
        prefs(ctx).edit().clear().apply()
    }
}
