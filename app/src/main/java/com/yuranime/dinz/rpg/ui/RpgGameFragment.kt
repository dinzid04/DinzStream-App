package com.yuranime.dinz.rpg.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.badlogic.gdx.backends.android.SafeAndroidFragmentApplication

class RpgGameFragment : SafeAndroidFragmentApplication() {

    companion object {
        private const val TAG = "RpgGameFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val hostActivity = activity as? RpgGameActivity
        if (hostActivity == null || !hostActivity.isSessionAuthorized) {
            return View(requireContext())
        }

        val game = hostActivity.getOrCreateGame()
        if (game == null) {
            return View(requireContext())
        }

        val config = AndroidApplicationConfiguration().apply {
            useAccelerometer = false
            useCompass = false
            useGyroscope = false
            useRotationVectorSensor = false
            useImmersiveMode = false
            useWakelock = true
            useGL30 = false
            numSamples = 0
        }

        return try {
            initializeForView(game, config).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Gagal inisialisasi LibGDX view: ${t.message}", t)
            hostActivity.onEngineInitFailure(t)
            View(requireContext())
        }
    }
}
