package com.yad.videoeditor

import android.app.Activity
import android.widget.Toast
import com.startapp.sdk.adsbase.Ad
import com.startapp.sdk.adsbase.StartAppAd
import com.startapp.sdk.adsbase.adlisteners.AdEventListener
import com.startapp.sdk.adsbase.adlisteners.VideoListener

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
        rewardGiven = false
        val startTime = System.currentTimeMillis()
        Tracker.adEvent("StartApp", "rewarded", "load_request")
        rewardedAd = StartAppAd(activity)

        rewardedAd?.setVideoListener(object : VideoListener {
            override fun onVideoCompleted() {
                Tracker.adEvent("StartApp", "rewarded", "video_completed")
                rewardGiven = true
                onRewardCallback?.invoke()
                cleanup()
            }
        })

        rewardedAd?.loadAd(
            StartAppAd.AdMode.REWARDED_VIDEO,
            object : AdEventListener {
                override fun onReceiveAd(ad: Ad) {
                    val duration = System.currentTimeMillis() - startTime
                    Tracker.adEvent("StartApp", "rewarded", "loaded", mapOf(
                        "duration_ms" to duration
                    ))
                    onLoaded()
                }
                override fun onFailedToReceiveAd(ad: Ad?) {
                    val duration = System.currentTimeMillis() - startTime
                    val errMsg = ad?.errorMessage ?: "no_fill"
                    Tracker.adEvent("StartApp", "rewarded", "load_failed", mapOf(
                        "duration_ms" to duration,
                        "error" to errMsg
                    ))
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
            Tracker.adEvent("StartApp", "rewarded", "show_failed_not_loaded")
            onFailed("Iklan belum siap. Memuat ulang...")
            loadRewarded(activity)
            return
        }

        Tracker.adEvent("StartApp", "rewarded", "show_called")
        rewardedAd?.showAd()
    }

    private fun cleanup() {
        rewardedAd = null
        rewardGiven = false
        onRewardCallback = null
        onFailedCallback = null
    }
}
