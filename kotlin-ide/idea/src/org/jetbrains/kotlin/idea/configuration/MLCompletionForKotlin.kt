/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.completion.ml.settings.CompletionMLRankingSettings
import org.jetbrains.kotlin.idea.KotlinBundle

object MLCompletionForKotlinFeature : ExperimentalFeature() {
    override val title: String
        get() = KotlinBundle.message("experimental.ml.completion")

    override fun shouldBeShown(): Boolean = MLCompletionForKotlin.isAvailable

    override var isEnabled: Boolean
        get() = MLCompletionForKotlin.isEnabled
        set(value) {
            MLCompletionForKotlin.isEnabled = value
        }
}

internal object MLCompletionForKotlin {
    const val isAvailable: Boolean = true

    var isEnabled: Boolean
        get() {
            val settings = CompletionMLRankingSettings.getInstance()
            return settings.isRankingEnabled && settings.isLanguageEnabled("Kotlin")
        }
        set(value) {
            val settings = CompletionMLRankingSettings.getInstance()
            if (value && !settings.isRankingEnabled) {
                settings.isRankingEnabled = true
            }

            settings.setLanguageEnabled("Kotlin", value)
        }
}
