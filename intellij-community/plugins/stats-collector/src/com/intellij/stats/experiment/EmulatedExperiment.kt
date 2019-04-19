// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.stats.experiment

import com.intellij.internal.statistic.DeviceIdManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.registry.Registry

/*
 * For now, we decide about AB experiment inside from IDE using user id and salt
 */
class EmulatedExperiment {
    companion object {
        const val GROUP_A_EXPERIMENT_VERSION: Int = 5
        const val GROUP_B_EXPERIMENT_VERSION: Int = 6
        const val IS_ENABLED = false

        fun shouldRank(experimentVersion: Int): Boolean {
            return experimentVersion == GROUP_B_EXPERIMENT_VERSION && !Registry.`is`("java.completion.ml.exit.experiment")
        }
    }

    fun emulate(experimentVersion: Int, performExperiment: Boolean, salt: String): Int? {
        val application = ApplicationManager.getApplication()
        if (!application.isEAP || application.isUnitTestMode || experimentVersion != 2 || performExperiment || !IS_ENABLED) {
            return null
        }

        val userId = DeviceIdManager.getOrGenerateId()
        val hash = (userId + salt).hashCode() % 10
        return when (hash) {
            3 -> GROUP_A_EXPERIMENT_VERSION
            4 -> GROUP_B_EXPERIMENT_VERSION
            else -> null
        }
    }
}