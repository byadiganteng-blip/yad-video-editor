package com.yad.videoeditor

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.startapp.sdk.ads.banner.Banner
import com.startapp.sdk.ads.banner.BannerListener
import com.startapp.sdk.adsbase.Ad
import com.startapp.sdk.adsbase.StartAppAd
import com.startapp.sdk.adsbase.StartAppSDK
import com.startapp.sdk.adsbase.adlisteners.AdDisplayListener
import com.startapp.sdk.adsbase.adlisteners.AdEventListener
import com.startapp.sdk.adsbase.model.AdPreferences

object StartAppHelper {

    private const val TAG = "StartAppHelper"
    private const val APP_ID = "208878110"

    private var lastInterstitialShown = 0L
    private const val INTERSTITIAL_COOLDOWN_MS = 60_000L

    fun init(context: Context) {
        try {
            StartAppSDK.init(context, APP_ID, false)
            Tracker.info(TAG, "sdk_initialized", data = mapOf("app_id" to APP_ID))
        } catch (e: Exception) {
            Tracker.error(TAG, "init_failed", "", e)
        }
    }

    fun loadBanner(activity: Activity, container: ViewGroup) {
        try {
            container.visibility = View.GONE
            val startTime = System.currentTimeMillis()
            Tracker.adEvent("StartApp", "banner", "load_request")

            val banner = Banner(activity, AdPreferences(), object : BannerListener {
                override fun onReceiveAd(view: View) {
                    val duration = System.currentTimeMillis() - startTime
                    Tracker.adEvent("StartApp", "banner", "loaded", mapOf(
                        "duration_ms" to duration
                    ))
                    try {
                        container.removeAllViews()
                        container.addView(view)
                        container.visibility = View.VISIBLE
                    } catch (e: Exception) {
                        Tracker.error(TAG, "banner_add_view_failed", "", e)
                    }
                }
                override fun onFailedToReceiveAd(view: View) {
                    val duration = System.currentTimeMillis() - startTime
                    Tracker.adEvent("StartApp", "banner", "load_failed", mapOf(
                        "duration_ms" to duration,
                        "reason" to "no_fill_or_error"
                    ))
                    container.visibility = View.GONE
                }
                override fun onImpression(view: View?) {
                    Tracker.adEvent("StartApp", "banner", "impression")
                }
                override fun onClick(view: View?) {
                    Tracker.adEvent("StartApp", "banner", "clicked")
                }
            })
            banner.loadAd(320, 50)
        } catch (e: Exception) {
            Tracker.error(TAG, "load_banner_failed", "", e)
            container.visibility = View.GONE
        }
    }

    fun showInterstitial(activity: Activity, onDismiss: () -> Unit = {}) {
        val now = System.currentTimeMillis()
        val sinceLast = now - lastInterstitialShown

        if (sinceLast < INTERSTITIAL_COOLDOWN_MS) {
            Tracker.adEvent("StartApp", "interstitial", "cooldown_skip", mapOf(
                "since_last_ms" to sinceLast,
                "cooldown_ms" to INTERSTITIAL_COOLDOWN_MS
            ))
            onDismiss()
            return
        }

        try {
            val startTime = System.currentTimeMillis()
            Tracker.adEvent("StartApp", "interstitial", "load_request")

            val ad = StartAppAd(activity)
            ad.loadAd(AdPreferences(), object : AdEventListener {
                override fun onReceiveAd(ad: Ad) {
                    val loadDuration = System.currentTimeMillis() - startTime
                    Tracker.adEvent("StartApp", "interstitial", "loaded", mapOf(
                        "duration_ms" to loadDuration
                    ))

                    if (ad is StartAppAd) {
                        val showStart = System.currentTimeMillis()
                        Tracker.adEvent("StartApp", "interstitial", "show_called")

                        ad.showAd(object : AdDisplayListener {
                            override fun adHidden(ad: Ad) {
                                val showDuration = System.currentTimeMillis() - showStart
                                lastInterstitialShown = System.currentTimeMillis()
                                Tracker.adEvent("StartApp", "interstitial", "dismissed", mapOf(
                                    "visible_duration_ms" to showDuration
                                ))
                                onDismiss()
                            }
                            override fun adDisplayed(ad: Ad) {
                                Tracker.adEvent("StartApp", "interstitial", "displayed")
                            }
                            override fun adClicked(ad: Ad) {
                                Tracker.adEvent("StartApp", "interstitial", "clicked")
                            }
                            override fun adNotDisplayed(ad: Ad) {
                                Tracker.adEvent("StartApp", "interstitial", "not_displayed")
                                onDismiss()
                            }
                        })
                    } else {
                        Tracker.adEvent("StartApp", "interstitial", "cast_failed")
                        onDismiss()
                    }
                }
                override fun onFailedToReceiveAd(ad: Ad?) {
                    val duration = System.currentTimeMillis() - startTime
                    val errMsg = ad?.errorMessage ?: "unknown"
                    Tracker.adEvent("StartApp", "interstitial", "load_failed", mapOf(
                        "duration_ms" to duration,
                        "error" to errMsg
                    ))
                    onDismiss()
                }
            })
        } catch (e: Exception) {
            Tracker.error(TAG, "show_interstitial_failed", "", e)
            onDismiss()
        }
    }
}
