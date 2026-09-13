package com.yad.videoeditor

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * Manager semua iklan AdMob.
 *
 * CARA PAKAI:
 *   AdMobHelper.init(context)
 *   AdMobHelper.showRewarded(activity) { berhasil -> ... }
 *   AdMobHelper.showInterstitial(activity) { ... }
 */
object AdMobHelper {

    private const val TAG = "AdMobHelper"

    // ⚠️ GANTI dengan Ad Unit ID dari AdMob Anda!
    // Test IDs dari Google (boleh dipakai untuk testing):
    private const val REWARDED_AD_UNIT = "ca-app-pub-2515513620924097/1056594580"
    private const val INTERSTITIAL_AD_UNIT = "ca-app-pub-2515513620924097/6892596385"

    // State
    private var rewardedAd: RewardedAd? = null
    private var interstitialAd: InterstitialAd? = null

    // Limit biar tidak spam iklan interstitial
    private var lastInterstitialShown = 0L
    private const val INTERSTITIAL_COOLDOWN_MS = 60_000L // 1 menit

    // ============================================================
    //  INIT — load rewarded + interstitial
    // ============================================================
    fun init(context: Context) {
        loadRewarded(context)
        loadInterstitial(context)
    }

    // ============================================================
    //  REWARDED AD
    // ============================================================
    fun loadRewarded(context: Context) {
        RewardedAd.load(
            context, REWARDED_AD_UNIT,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    Log.d(TAG, "✅ Rewarded loaded")
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    Log.e(TAG, "❌ Rewarded failed: ${error.message}")
                }
            }
        )
    }

    fun showRewarded(activity: Activity, onReward: (Boolean) -> Unit) {
        val ad = rewardedAd
        if (ad == null) {
            Toast.makeText(activity, "Iklan belum siap, coba lagi...",
                Toast.LENGTH_SHORT).show()
            loadRewarded(activity)
            onReward(false)
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                loadRewarded(activity)
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewardedAd = null
                loadRewarded(activity)
            }
        }

        ad.show(activity) { rewardItem ->
            Log.d(TAG, "🎁 Reward: ${rewardItem.amount} ${rewardItem.type}")
            onReward(true)
        }
    }

    // ============================================================
    //  INTERSTITIAL AD
    // ============================================================
    fun loadInterstitial(context: Context) {
        InterstitialAd.load(
            context, INTERSTITIAL_AD_UNIT,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    Log.d(TAG, "✅ Interstitial loaded")
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    Log.e(TAG, "❌ Interstitial failed: ${error.message}")
                }
            }
        )
    }

    fun showInterstitial(activity: Activity, onDismiss: () -> Unit = {}) {
        // Cooldown agar tidak spam
        val now = System.currentTimeMillis()
        if (now - lastInterstitialShown < INTERSTITIAL_COOLDOWN_MS) {
            onDismiss()
            return
        }

        val ad = interstitialAd
        if (ad == null) {
            onDismiss()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                loadInterstitial(activity)
                lastInterstitialShown = System.currentTimeMillis()
                onDismiss()
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitialAd = null
                loadInterstitial(activity)
                onDismiss()
            }
        }

        ad.show(activity)
    }
}
