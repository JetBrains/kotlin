/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters

/**
 * Obtain all System environment variables.
 *
 * Use this to fetch the environment variables without registering them as Configuration Cache inputs.
 * (Gradle automatically intercepts calls to [System.getenv] and registers the values as CC inputs,
 * _unless_ done inside a [ValueSource].)
 */
internal abstract class SystemGetEnvSource :
    ValueSource<Map<String, String>, ValueSourceParameters.None> {

    override fun obtain(): Map<String, String> = System.getenv()

    internal companion object {
        /**
         * Fetch all System environment variables without registering them as Configuration Cache inputs.
         *
         * @see SystemGetEnvSource
         */
        fun ProviderFactory.getAllEnvironmentVariables(): Provider<Map<String, String>> =
            of(SystemGetEnvSource::class.java) { }
    }
}
