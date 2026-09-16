package com.yad.videoeditor

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.yad.videoeditor.databinding.ActivityRewardBinding

class RewardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRewardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRewardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "🎁 Reward"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Preload ad saat activity dibuka
        StartAppRewardedHelper.preload(this)

        updateUI()

        // ============================================================
        //  REWARD: PREMIUM
        // ============================================================
        binding.cardPremium.setOnClickListener {
            if (RewardManager.isPremium(this)) {
                Toast.makeText(this, "✅ Premium sudah aktif", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showRewarded("premium")
        }

        // ============================================================
        //  REWARD: WATERMARK
        // ============================================================
        binding.cardWatermark.setOnClickListener {
            if (RewardManager.isWatermarkRemoved(this)) {
                Toast.makeText(this, "✅ Watermark sudah dihapus", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showRewarded("watermark")
        }

        // ============================================================
        //  REWARD: CREDITS
        // ============================================================
        binding.cardCredits.setOnClickListener {
            showRewarded("credits")
        }

        // ============================================================
        //  REWARD: FILTER
        // ============================================================
        binding.cardFilter.setOnClickListener {
            showRewarded("filter")
        }
    }

    private fun showRewarded(rewardType: String) {
        // Disable semua card dulu
        setCardsEnabled(false)

        StartAppRewardedHelper.loadRewarded(
            activity = this,
            onLoaded = {
                runOnUiThread {
                    StartAppRewardedHelper.showRewarded(
                        activity = this,
                        onReward = {
                            runOnUiThread {
                                giveReward(rewardType)
                                setCardsEnabled(true)
                                updateUI()
                            }
                        },
                        onFailed = { msg ->
                            runOnUiThread {
                                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                                setCardsEnabled(true)
                            }
                        }
                    )
                }
            },
            onFailed = { msg ->
                runOnUiThread {
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                    setCardsEnabled(true)
                }
            }
        )
    }

    private fun giveReward(type: String) {
        when (type) {
            "premium" -> {
                RewardManager.unlockPremium(this)
                Toast.makeText(this, "🎉 Premium unlocked!", Toast.LENGTH_LONG).show()
            }
            "watermark" -> {
                RewardManager.removeWatermark(this)
                Toast.makeText(this, "🎉 Watermark dihapus!", Toast.LENGTH_LONG).show()
            }
            "credits" -> {
                RewardManager.addCredits(this, 10)
                Toast.makeText(this, "🎉 +10 credits!", Toast.LENGTH_LONG).show()
            }
            "filter" -> {
                RewardManager.unlockFilter(this, "premium_filter_1")
                Toast.makeText(this, "🎉 Filter premium unlocked!", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setCardsEnabled(enabled: Boolean) {
        binding.cardPremium.isEnabled = enabled
        binding.cardWatermark.isEnabled = enabled
        binding.cardCredits.isEnabled = enabled
        binding.cardFilter.isEnabled = enabled
    }

    private fun updateUI() {
        // Premium
        if (RewardManager.isPremium(this)) {
            binding.tvPremiumStatus.text = "✅ Aktif"
            binding.tvPremiumStatus.setTextColor(0xFF10B981.toInt())
        } else {
            binding.tvPremiumStatus.text = "🔒 Belum aktif"
            binding.tvPremiumStatus.setTextColor(0xFF9A96B8.toInt())
        }

        // Watermark
        if (RewardManager.isWatermarkRemoved(this)) {
            binding.tvWatermarkStatus.text = "✅ Aktif"
            binding.tvWatermarkStatus.setTextColor(0xFF10B981.toInt())
        } else {
            binding.tvWatermarkStatus.text = "🔒 Belum aktif"
            binding.tvWatermarkStatus.setTextColor(0xFF9A96B8.toInt())
        }

        // Credits
        binding.tvCreditsStatus.text = "💰 ${RewardManager.getCredits(this)} credits"

        // Filter
        val filterCount = RewardManager.getUnlockedFilters(this).size
        binding.tvFilterStatus.text = "🎨 $filterCount filter unlocked"
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
