package com.yad.videoeditor

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * PermissionHelper — otomatis minta izin:
 *   1. Storage (READ/WRITE/MEDIA)
 *   2. Floating window (SYSTEM_ALERT_WINDOW)
 *   3. Notification (Android 13+)
 *
 * Cara pakai (di MainActivity.onCreate):
 *   PermissionHelper.requestAllPermissions(this)
 */
object PermissionHelper {

    private const val TAG = "PermissionHelper"
    const val REQ_CODE_STORAGE = 1001
    const val REQ_CODE_NOTIFICATION = 1002

    /**
     * Minta semua izin yang dibutuhkan (storage + floating + notification).
     */
    fun requestAllPermissions(activity: Activity) {
        try {
            // 1. Minta izin storage
            requestStoragePermissions(activity)

            // 2. Minta izin floating (SYSTEM_ALERT_WINDOW)
            requestFloatingPermission(activity)

            // 3. Minta izin notification (Android 13+)
            requestNotificationPermission(activity)

        } catch (e: Exception) {
            AutoLogSaver.logError(TAG, "requestAllPermissions failed", e)
        }
    }

    // ============================================================
    //  STORAGE PERMISSIONS
    // ============================================================
    private fun requestStoragePermissions(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestAndroid13Permissions(activity)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requestManageExternalStorage(activity)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestLegacyStorage(activity)
        }
    }

    private fun requestAndroid13Permissions(activity: Activity) {
        val permissions = mutableListOf<String>()
        if (!hasPermission(activity, Manifest.permission.READ_MEDIA_IMAGES))
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
        if (!hasPermission(activity, Manifest.permission.READ_MEDIA_VIDEO))
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
        if (!hasPermission(activity, Manifest.permission.READ_MEDIA_AUDIO))
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)

        if (permissions.isNotEmpty()) {
            AutoLogSaver.log(TAG, "Requesting Android 13+ storage: $permissions")
            ActivityCompat.requestPermissions(activity, permissions.toTypedArray(), REQ_CODE_STORAGE)
        }
    }

    private fun requestManageExternalStorage(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                AutoLogSaver.log(TAG, "Requesting MANAGE_EXTERNAL_STORAGE")
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:" + activity.packageName)
                    activity.startActivity(intent)
                } catch (e: Exception) {
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        activity.startActivity(intent)
                    } catch (_: Exception) {}
                }
            }
        }
    }

    private fun requestLegacyStorage(activity: Activity) {
        val permissions = mutableListOf<String>()
        if (!hasPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE))
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            if (!hasPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE))
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (permissions.isNotEmpty()) {
            AutoLogSaver.log(TAG, "Requesting legacy storage: $permissions")
            ActivityCompat.requestPermissions(activity, permissions.toTypedArray(), REQ_CODE_STORAGE)
        }
    }

    // ============================================================
    //  FLOATING PERMISSION (SYSTEM_ALERT_WINDOW)
    // ============================================================
    /**
     * Minta izin floating (display over other apps).
     * Android tidak kasih popup — kita buka Settings manual.
     */
    fun requestFloatingPermission(activity: Activity) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(activity)) {
                    AutoLogSaver.log(TAG, "Requesting SYSTEM_ALERT_WINDOW")
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + activity.packageName)
                    )
                    activity.startActivity(intent)
                } else {
                    AutoLogSaver.log(TAG, "Floating permission already granted")
                }
            }
        } catch (e: Exception) {
            AutoLogSaver.logError(TAG, "requestFloatingPermission failed", e)
        }
    }

    /**
     * Cek apakah izin floating sudah diberikan.
     */
    fun hasFloatingPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true // Android < 6 auto granted
        }
    }

    // ============================================================
    //  NOTIFICATION PERMISSION (Android 13+)
    // ============================================================
    private fun requestNotificationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasPermission(activity, Manifest.permission.POST_NOTIFICATIONS)) {
                AutoLogSaver.log(TAG, "Requesting POST_NOTIFICATIONS")
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQ_CODE_NOTIFICATION
                )
            }
        }
    }

    // ============================================================
    //  HELPERS
    // ============================================================
    private fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) ==
                PackageManager.PERMISSION_GRANTED
    }

    fun hasAllPermissions(context: Context): Boolean {
        return try {
            val storageOk = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                    hasPermission(context, Manifest.permission.READ_MEDIA_IMAGES)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
                    Environment.isExternalStorageManager()
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
                    hasPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                else -> true
            }
            storageOk && hasFloatingPermission(context)
        } catch (e: Exception) {
            false
        }
    }
}
