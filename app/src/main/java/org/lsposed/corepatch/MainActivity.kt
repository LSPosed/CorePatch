package org.lsposed.corepatch

import android.app.Activity
import android.os.Bundle
import android.widget.ListView
import org.lsposed.corepatch.App.Companion.mService
import org.lsposed.corepatch.App.Companion.reloadListener
import org.lsposed.corepatch.adapter.MultiTypeListAdapter
import org.lsposed.corepatch.data.SwitchData

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (mService != null) {
            loadPrefs()
        } else {
            reloadListener = {
                loadPrefs()
            }
        }
    }

    private fun loadPrefs() {
        val bypassDowngrade = SwitchData(
            getString(R.string.bypass_downgrade), "", Config.BYPASS_DOWNGRADE
        )
        val bypassVerification = SwitchData(
            getString(R.string.bypass_verification), "", Config.BYPASS_VERIFICATION
        )
        val bypassDigest = SwitchData(
            getString(R.string.bypass_digest), "", Config.BYPASS_DIGEST
        )
        val enhancedMode = SwitchData(
            getString(R.string.enhanced_mode), "", Config.ENHANCED_MODE
        )
        val usePreviousSignatures = SwitchData(
            getString(R.string.use_previous_signatures), "", Config.USE_PREVIOUS_SIGNATURES
        )
        val bypassSharedUser = SwitchData(
            getString(R.string.bypass_shared_user), "", Config.BYPASS_SHARED_USER
        )
        val disableVerificationAgent = SwitchData(
            getString(R.string.disable_verification_agent), "", Config.DISABLE_VERIFICATION_AGENT
        )

        val dataSet = arrayListOf(
            bypassDowngrade,
            bypassVerification,
            bypassDigest,
            enhancedMode,
            usePreviousSignatures,
            bypassSharedUser,
            disableVerificationAgent
        )

        val adapter = MultiTypeListAdapter(dataSet)

        val listView = ListView(this)
        listView.adapter = adapter
        setContentView(listView)

        setContentView(listView)
    }

    override fun onStop() {
        super.onStop()
        reloadListener = {}
    }
}