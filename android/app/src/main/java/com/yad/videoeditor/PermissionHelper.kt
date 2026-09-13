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
 * PermissionHelper — otomatis minta izin storage saat app dibuka.
 *
 * Cara pakai (di MainActivity.onCreate):
 *   PermissionHelper.requestAllPermissions(this)
 *
 * Fitur:
 *   - Android 13+ (API 33): READ_MEDIA_IMAGES, READ_MEDIA_VIDEO, READ_MEDIA_AUDIO
 *   - Android 11-12 (API 30-32): MANAGE_EXTERNAL_STORAGE
 *   - Android 6-10 (API 23-29): READ/WRITE_EXTERNAL_STORAGE
 */
object PermissionHelper {

    private const val TAG = "PermissionHelper"
    const val REQ_CODE_STORAGE = 1001

    /**
     * Minta semua izin yang dibutuhkan.
     */
    fun requestAllPermissions(activity: Activity) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ (API 33+)
                requestAndroid13Permissions(activity)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11-12 (API 30-32)
                requestManageExternalStorage(activity)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Android 6-10 (API 23-29)
                requestLegacyStorage(activity)
            }
            // Android < 6: izin otomatis granted saat install
        } catch (e: Exception) {
            AutoLogSaver.logError(TAG, "requestAllPermissions failed", e)
        }
    }

    /**
     * Android 13+ — minta izin media baru.
     */
    private fun requestAndroid13Permissions(activity: Activity) {
        val permissions = mutableListOf<String>()

        if (!hasPermission(activity, Manifest.permission.READ_MEDIA_IMAGES)) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
        }
        if (!hasPermission(activity, Manifest.permission.READ_MEDIA_VIDEO)) {
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
        }
        if (!hasPermission(activity, Manifest.permission.READ_MEDIA_AUDIO)) {
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
        }
        // WRITE_EXTERNAL_STORAGE tidak perlu di Android 13+ untuk app-specific dir

        if (permissions.isNotEmpty()) {
            AutoLogSaver.log(TAG, "Requesting Android 13+ permissions: $permissions")
            ActivityCompat.requestPermissions(
                activity,
                permissions.toTypedArray(),
                REQ_CODE_STORAGE
            )
        } else {
            AutoLogSaver.log(TAG, "All Android 13+ permissions already granted")
        }
    }

    /**
     * Android 11-12 — minta MANAGE_EXTERNAL_STORAGE via Settings.
     */
    private fun requestManageExternalStorage(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                AutoLogSaver.log(TAG, "Requesting MANAGE_EXTERNAL_STORAGE")
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:" + activity.packageName)
                    activity.startActivity(intent)
                } catch (e: Exception) {
                    // Fallback ke halaman Settings umum
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    activity.startActivity(intent)
                }
            } else {
                AutoLogSaver.log(TAG, "MANAGE_EXTERNAL_STORAGE already granted")
            }
        }
    }

    /**
     * Android 6-10 — minta izin storage lama.
     */
    private fun requestLegacyStorage(activity: Activity) {
        val permissions = mutableListOf<String>()

        if (!hasPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            // WRITE_EXTERNAL_STORAGE hanya perlu di Android 9 ke bawah
            if (!hasPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        if (permissions.isNotEmpty()) {
            AutoLogSaver.log(TAG, "Requesting legacy permissions: $permissions")
            ActivityCompat.requestPermissions(
                activity,
                permissions.toTypedArray(),
                REQ_CODE_STORAGE
            )
        } else {
            AutoLogSaver.log(TAG, "All legacy permissions already granted")
        }
    }

    /**
     * Cek apakah izin sudah diberikan.
     */
    private fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) ==
                PackageManager.PERMISSION_GRANTED
    }

    /**
     * Cek apakah semua izin sudah diberikan (untuk dipakai di tempat lain).
     */
    fun hasAllPermissions(context: Context): Boolean {
        return try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                    hasPermission(context, Manifest.permission.READ_MEDIA_IMAGES) &&
                    hasPermission(context, Manifest.permission.READ_MEDIA_VIDEO)
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                    Environment.isExternalStorageManager()
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    hasPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                }
                else -> true
            }
        } catch (e: Exception) {
            false
        }
    }
}
