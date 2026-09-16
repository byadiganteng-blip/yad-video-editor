package com.yad.videoeditor

import android.app.Activity
import android.util.Log
import android.widget.Toast
import com.startapp.sdk.adsbase.Ad
import com.startapp.sdk.adsbase.StartAppAd
import com.startapp.sdk.adsbase.adlisteners.AdDisplayListener
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
        Log.d(TAG, "Loading rewarded ad...")
        rewardedAd = StartAppAd(activity)

        rewardedAd?.loadAd(
            StartAppAd.AdMode.REWARDED_VIDEO,
            object : AdEventListener {

                override fun onReceiveAd(ad: Ad) {
                    Log.d(TAG, "Rewarded ad loaded")

                    ad.setAdDisplayListener(object : AdDisplayListener {
                        override fun adHidden(ad: Ad) {
                            Log.d(TAG, "Ad hidden")
                            if (rewardGiven) {
                                Log.d(TAG, "Reward diberikan")
                                onRewardCallback?.invoke()
                            } else {
                                Log.d(TAG, "User skip, tidak dapat reward")
                                onFailedCallback?.invoke("Video tidak selesai. Tidak dapat reward.")
                            }
                            cleanup()
                        }

                        override fun adDisplayed(ad: Ad) {
                            Log.d(TAG, "Ad displayed")
                        }

                        override fun adClicked(ad: Ad) {
                            Log.d(TAG, "Ad clicked")
                        }

                        override fun adNotDisplayed(ad: Ad) {
                            Log.d(TAG, "Ad not displayed")
                        }
                    })

                    ad.setVideoListener(object : VideoListener {
                        override fun onVideoCompleted() {
                            Log.d(TAG, "Video completed")
                            rewardGiven = true
                        }
                    })

                    onLoaded()
                }

                override fun onFailedToReceiveAd(ad: Ad?) {
                    Log.e(TAG, "Rewarded ad failed to load: " + (ad?.errorMessage ?: "unknown"))
                    onFailed("Iklan tidak tersedia. Coba lagi nanti.")
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

        rewardedAd?.show()
    }

    private fun cleanup() {
        rewardedAd = null
        rewardGiven = false
        onRewardCallback = null
        onFailedCallback = null
        Log.d(TAG, "Cleanup selesai")
    }
}
