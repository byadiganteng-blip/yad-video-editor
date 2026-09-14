package com.yad.videoeditor

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import com.startapp.sdk.ads.banner.Banner
import com.startapp.sdk.ads.banner.BannerListener
import com.startapp.sdk.adsbase.StartAppAd
import com.startapp.sdk.adsbase.StartAppSDK
import com.startapp.sdk.adsbase.adlisteners.AdDisplayListener
import com.startapp.sdk.adsbase.adlisteners.AdEventListener
import com.startapp.sdk.adsbase.model.AdPreferences

/**
 * StartAppHelper — iklan StartApp (banner + interstitial).
 *
 * Dipakai sebagai alternatif AdMob yang tidak butuh Play Store.
 * App ID: 208878110
 */
object StartAppHelper {

    private const val TAG = "StartAppHelper"
    private const val APP_ID = "208878110"

    private var lastInterstitialShown = 0L
    private const val INTERSTITIAL_COOLDOWN_MS = 60_000L

    /**
     * Init StartApp SDK — panggil di MainActivity.onCreate()
     */
    fun init(context: Context) {
        try {
            StartAppSDK.init(context, APP_ID, false)
            AutoLogSaver.log(TAG, "StartApp initialized")
        } catch (e: Exception) {
            AutoLogSaver.logError(TAG, "init failed", e)
        }
    }

    /**
     * Load banner ke container FrameLayout.
     */
    fun loadBanner(activity: Activity, container: ViewGroup) {
        try {
            val banner = Banner(activity, 320, 50)
            banner.setBannerListener(object : BannerListener {
                override fun onReceiveAd(view: View) {
                    AutoLogSaver.log(TAG, "Banner loaded")
                    try {
                        container.removeAllViews()
                        container.addView(view)
                    } catch (_: Exception) {}
                }
                override fun onFailedToReceiveAd(view: View) {
                    AutoLogSaver.log(TAG, "Banner failed")
                }
                override fun onImpression(view: View?) {}
                override fun onClick(view: View?) {}
            })
            banner.loadAd(320, 50)
        } catch (e: Exception) {
            AutoLogSaver.logError(TAG, "loadBanner failed", e)
        }
    }

    /**
     * Show interstitial — panggil setelah aksi penting.
     */
    fun showInterstitial(activity: Activity, onDismiss: () -> Unit = {}) {
        val now = System.currentTimeMillis()
        if (now - lastInterstitialShown < INTERSTITIAL_COOLDOWN_MS) {
            onDismiss()
            return
        }

        try {
            val ad = StartAppAd(activity)
            val prefs = AdPreferences()
            ad.loadAd(prefs, object : AdEventListener {
                override fun onReceiveAd(ad: StartAppAd) {
                    ad.showAd(object : AdDisplayListener {
                        override fun adHidden(ad: StartAppAd?) {
                            lastInterstitialShown = System.currentTimeMillis()
                            onDismiss()
                        }
                        override fun adDisplayed(ad: StartAppAd?) {}
                        override fun adClicked(ad: StartAppAd?) {}
                        override fun adNotDisplayed(ad: StartAppAd?) {
                            onDismiss()
                        }
                    })
                }
                override fun onFailedToReceiveAd(ad: StartAppAd?) {
                    onDismiss()
                }
            })
        } catch (e: Exception) {
            AutoLogSaver.logError(TAG, "showInterstitial failed", e)
            onDismiss()
        }
    }
}
