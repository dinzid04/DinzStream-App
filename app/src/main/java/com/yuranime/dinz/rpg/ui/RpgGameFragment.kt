package com.yuranime.dinz.rpg.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.badlogic.gdx.backends.android.AndroidFragmentApplication
import com.yuranime.dinz.rpg.engine.CozyFishingGame

class RpgGameFragment : AndroidFragmentApplication() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val hostActivity = activity as? RpgGameActivity
        val game = hostActivity?.getOrCreateGame()
            ?: CozyFishingGame(requireContext().applicationContext) {}

        val config = AndroidApplicationConfiguration().apply {
            useAccelerometer = false
            useCompass = false
            useGyroscope = false
            useImmersiveMode = true
            numSamples = 0
        }
        return initializeForView(game, config)
    }
}
