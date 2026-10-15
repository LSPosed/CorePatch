package org.lsposed.corepatch

import org.lsposed.corepatch.App.Companion.rwPrefs
import org.lsposed.corepatch.XposedHelper.prefs

object Config {
    const val BYPASS_DOWNGRADE = "downgrade"
    const val BYPASS_MIN_SDK = "bypass_min_sdk"
    const val BYPASS_VERIFICATION = "bypass_verification"
    const val BYPASS_RESOURCE_ARSC_RESTRICTIONS = "bypass_resource_arsc_restrictions"
    const val BYPASS_DIGEST = "bypass_digest"
    const val BYPASS_EXACT_SIGNATURE_MATCH = "bypass_exact_sig_match"
    const val USE_PREVIOUS_SIGNATURES = "use_previous_signatures"
    const val ALLOW_HIDDEN_APIS_FOR_SYSTEM_APPS = "allow_hidden_apis_for_system_apps"
    const val BYPASS_SHARED_USER = "bypass_shared_user"
    const val DISABLE_VERIFICATION_AGENT = "disable_verification_agent"
    const val BYPASS_BLOCK = "bypass_block"

    private val allConfig = arrayOf(
        BYPASS_DOWNGRADE,
        BYPASS_MIN_SDK,
        BYPASS_VERIFICATION,
        BYPASS_RESOURCE_ARSC_RESTRICTIONS,
        BYPASS_DIGEST,
        USE_PREVIOUS_SIGNATURES,
        ALLOW_HIDDEN_APIS_FOR_SYSTEM_APPS,
        BYPASS_SHARED_USER,
        BYPASS_BLOCK
    )

    fun printAllConfig() {
        allConfig.forEach {
            XposedHelper.log("$it: ${prefs.getBoolean(it, false)}")
        }
    }

    fun isBypassDowngradeEnabled(): Boolean {
        return prefs.getBoolean(BYPASS_DOWNGRADE, false)
    }

    fun isBypassMinSdkEnabled(): Boolean {
        return prefs.getBoolean(BYPASS_MIN_SDK, false)
    }

    fun isBypassVerificationEnabled(): Boolean {
        return prefs.getBoolean(BYPASS_VERIFICATION, false)
    }

    fun isBypassResourceArscRestrictionsEnabled(): Boolean {
        return prefs.getBoolean(BYPASS_RESOURCE_ARSC_RESTRICTIONS, false)
    }

    fun isBypassDigestEnabled(): Boolean {
        return prefs.getBoolean(BYPASS_DIGEST, false)
    }

    fun isBypassExactSignatureMatch(): Boolean {
        return prefs.getBoolean(BYPASS_EXACT_SIGNATURE_MATCH, false)
    }

    fun isUsePreviousSignaturesEnabled(): Boolean {
        return prefs.getBoolean(USE_PREVIOUS_SIGNATURES, false)
    }

    fun isAllowHiddenApisForSystemAppsEnabled(): Boolean {
        return prefs.getBoolean(ALLOW_HIDDEN_APIS_FOR_SYSTEM_APPS, false)
    }

    fun isBypassSharedUserEnabled(): Boolean {
        return prefs.getBoolean(BYPASS_SHARED_USER, false)
    }

    fun isDisableVerificationAgentEnabled(): Boolean {
        return prefs.getBoolean(DISABLE_VERIFICATION_AGENT, false)
    }

    fun isBypassBlockEnabled(): Boolean {
        return prefs.getBoolean(BYPASS_BLOCK, false)
    }

    fun getConfig(key: String): Boolean {
        return rwPrefs?.getBoolean(key, false) ?: false
    }

    fun setConfig(key: String, value: Boolean) {
        rwPrefs?.edit()?.putBoolean(key, value)?.apply()
    }
}
