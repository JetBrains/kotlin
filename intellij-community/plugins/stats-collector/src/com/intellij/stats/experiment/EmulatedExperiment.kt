// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.stats.experiment

import com.intellij.ide.util.PropertiesComponent
import com.intellij.internal.statistic.DeviceIdManager
import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.registry.Registry
import kotlin.math.abs

/*
 * For now, we decide about AB experiment inside IDE using user id and salt
 */
class EmulatedExperiment {
    companion object {
        const val GROUP_A_EXPERIMENT_VERSION: Int = 7
        const val GROUP_B_EXPERIMENT_VERSION: Int = 8
        const val GROUP_KT_WITH_DIFF_EXPERIMENT_VERSION: Int = 9

        const val DIFF_ENABLED_PROPERTY_KEY = "ml.completion.diff.registry.was.enabled"

        const val IS_ENABLED = true

        fun shouldRank(language: Language, experimentVersion: Int): Boolean {
            return (
                     experimentVersion == GROUP_B_EXPERIMENT_VERSION ||
                     experimentVersion == GROUP_KT_WITH_DIFF_EXPERIMENT_VERSION && language.isKotlin()
                   )
                   && !Registry.`is`("completion.stats.exit.experiment")
        }

        private fun Language.isKotlin() = this.id == "Kotlin"
    }

    fun emulate(experimentVersion: Int, performExperiment: Boolean, salt: String): Int? {
        val application = ApplicationManager.getApplication()
        if (!application.isEAP || application.isUnitTestMode || experimentVersion != 2 || performExperiment || !IS_ENABLED) {
            return null
        }

        val userId = DeviceIdManager.getOrGenerateId()
        val hash = abs((userId + salt).hashCode()) % 8
        return when (hash) {
            3 -> GROUP_A_EXPERIMENT_VERSION
            4 -> GROUP_B_EXPERIMENT_VERSION
            5 -> GROUP_KT_WITH_DIFF_EXPERIMENT_VERSION.apply { enableOnceDiffShowing() }
            else -> null
        }
    }

    private fun enableOnceDiffShowing() {
        val properties = PropertiesComponent.getInstance()
        if (!properties.getBoolean(DIFF_ENABLED_PROPERTY_KEY, false)) {
            Registry.get("completion.stats.show.ml.ranking.diff").setValue(true)
            properties.setValue(DIFF_ENABLED_PROPERTY_KEY, true)
        }
    }
}