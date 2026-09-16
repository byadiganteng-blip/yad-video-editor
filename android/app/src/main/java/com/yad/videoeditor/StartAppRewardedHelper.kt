package com.yad.videoeditor

import android.app.Activity
import android.util.Log
import android.widget.Toast
import com.startapp.sdk.adsbase.Ad
import com.startapp.sdk.adsbase.StartAppAd
import com.startapp.sdk.adsbase.adlisteners.AdDisplayListener
import com.startapp.sdk.adsbase.adlisteners.AdEventListener
import com.startapp.sdk.adsbase.adlisteners.VideoListener

/**
 * Helper StartApp Rewarded Video — versi kompatibel SDK 4.10.12
 *
 * Catatan API 4.10.12:
 *  - StartAppAd.setVideoListener(VideoListener) → ADA
 *  - StartAppAd.setAdDisplayListener(...) → TIDAK ADA
 *  - Ad.setAdDisplayListener(...) → TIDAK ADA
 *  - StartAppAd.showAd() → ADA (bukan show())
 *
 * Deteksi reward: pakai VideoListener.onVideoCompleted()
 */
object StartAppRewardedHelper {

    private const val TAG = "StartAppRewarded"

    private var rewardedAd: StartAppAd? = null
    private var rewardGiven = false
    private var onRewardCallback: (() -> Unit)? = null
    private var onFailedCallback: ((String) -> Unit)? = null

    fun loadRewarded(
        activity: Activity,
        onLoaded: () -> Unit = {},
        onFailed: (String) -> Unit = {}
    ) {
        Log.d(TAG, "Loading rewarded ad...")
        rewardGiven = false
        rewardedAd = StartAppAd(activity)

        // Video listener harus di-set di StartAppAd (API 4.10.12)
        rewardedAd?.setVideoListener(object : VideoListener {
            override fun onVideoCompleted() {
                Log.d(TAG, "Video completed → reward diberikan")
                rewardGiven = true
                // Beri reward SETELAH video selesai
                onRewardCallback?.invoke()
                cleanup()
            }
        })

        rewardedAd?.loadAd(
            StartAppAd.AdMode.REWARDED_VIDEO,
            object : AdEventListener {

                override fun onReceiveAd(ad: Ad) {
                    Log.d(TAG, "Rewarded ad loaded")
                    onLoaded()
                }

                override fun onFailedToReceiveAd(ad: Ad?) {
                    val msg = ad?.errorMessage ?: "unknown"
                    Log.e(TAG, "Rewarded ad failed to load: $msg")
                    onFailed("Iklan tidak tersedia. Coba lagi nanti.")
                    cleanup()
                }
            }
        )
    }

    fun showRewarded(
        activity: Activity,
        onReward: () -> Unit,
        onFailed: (String) -> Unit = { msg ->
            Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
        }
    ) {
        rewardGiven = false
        onRewardCallback = onReward
        onFailedCallback = onFailed

        if (rewardedAd == null) {
            Log.e(TAG, "Ad belum di-load.")
            onFailed("Iklan belum siap. Memuat ulang...")
            loadRewarded(activity)
            return
        }

        // API 4.10.12: showAd(), bukan show()
        rewardedAd?.showAd()
    }

    private fun cleanup() {
        rewardedAd = null
        rewardGiven = false
        onRewardCallback = null
        onFailedCallback = null
        Log.d(TAG, "Cleanup selesai")
    }
}
