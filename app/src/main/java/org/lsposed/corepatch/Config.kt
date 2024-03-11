package org.lsposed.corepatch

import org.lsposed.corepatch.App.Companion.rwPrefs
import org.lsposed.corepatch.XposedHelper.prefs

object Config {
    const val BYPASS_DOWNGRADE = "downgrade"
    const val BYPASS_VERIFICATION = "bypass_verification"
    const val BYPASS_DIGEST = "bypass_digest"
    const val ENHANCED_MODE = "enhanced_mode"
    const val USE_PREVIOUS_SIGNATURES = "use_previous_signatures"
    const val BYPASS_SHARED_USER = "bypass_shared_user"

    private val allConfig = arrayOf(
        BYPASS_DOWNGRADE,
        BYPASS_VERIFICATION,
        BYPASS_DIGEST,
        ENHANCED_MODE,
        USE_PREVIOUS_SIGNATURES,
        BYPASS_SHARED_USER
    )

    fun printAllConfig() {
        allConfig.forEach {
            XposedHelper.log("$it: ${prefs.getBoolean(it, false)}")
        }
    }

    fun isBypassDowngradeEnabled(): Boolean {
        return prefs.getBoolean(BYPASS_DOWNGRADE, false)
    }

    fun isBypassVerificationEnabled(): Boolean {
        return prefs.getBoolean(BYPASS_VERIFICATION, false)
    }

    fun isBypassDigestEnabled(): Boolean {
        return prefs.getBoolean(BYPASS_DIGEST, false)
    }

    fun isEnhancedModeEnabled(): Boolean {
        return prefs.getBoolean(ENHANCED_MODE, false)
    }

    fun isUsePreviousSignaturesEnabled(): Boolean {
        return prefs.getBoolean(USE_PREVIOUS_SIGNATURES, false)
    }

    fun isBypassSharedUserEnabled(): Boolean {
        return prefs.getBoolean(BYPASS_SHARED_USER, false)
    }

    fun getConfig(key: String): Boolean {
        return rwPrefs?.getBoolean(key, false) ?: false
    }

    fun setConfig(key: String, value: Boolean) {
        rwPrefs?.edit()?.putBoolean(key, value)?.apply()
    }
}