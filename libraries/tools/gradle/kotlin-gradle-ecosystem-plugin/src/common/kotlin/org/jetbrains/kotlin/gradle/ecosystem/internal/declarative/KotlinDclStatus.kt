/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.ecosystem.internal.declarative

import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory

internal const val DCL_STATUS_GRADLE_PROPERTY_NAME = "kotlin.dclEnabled"

internal enum class KotlinDclStatus(val value: String) {
    ENABLED("true"),
    DISABLED("false"),
}

internal val ProviderFactory.dclStatus: Provider<KotlinDclStatus>
    get() = gradleProperty(DCL_STATUS_GRADLE_PROPERTY_NAME)
        .map { value ->
            when {
                KotlinDclStatus.ENABLED.value.contentEquals(value, ignoreCase = true) -> KotlinDclStatus.ENABLED
                KotlinDclStatus.DISABLED.value.contentEquals(value, ignoreCase = true) -> KotlinDclStatus.DISABLED
                else -> throw IllegalArgumentException(
                    "Unknown value for '$DCL_STATUS_GRADLE_PROPERTY_NAME' gradle property. Change it to one of ${
                        KotlinDclStatus.values().joinToString { it.value }
                    }"
                )
            }
        }
        .orElse(KotlinDclStatus.DISABLED)