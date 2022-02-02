/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.Project
import org.jetbrains.kotlin.commonizer.*
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider

internal val Project.commonizerLogLevel: CommonizerLogLevel
    get() {
        PropertiesProvider(this).commonizerLogLevel?.let { logLevelString ->
            val matchingLevel = CommonizerLogLevel.values().firstOrNull { logLevel -> logLevel.name.equals(logLevelString, true) }
            if (matchingLevel != null) return matchingLevel
        }

        return if (logger.isInfoEnabled) CommonizerLogLevel.Info else CommonizerLogLevel.Quiet
    }

internal val Project.additionalCommonizerSettings: List<AdditionalCommonizerSetting<*>>
    get() = listOf(
        OptimisticNumberCommonizationEnabledKey setTo isOptimisticNumberCommonizationEnabled,
        PlatformIntegerCommonizationEnabledKey setTo isPlatformIntegerCommonizationEnabled,
    )
