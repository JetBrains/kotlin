/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.target.PlatformManager
import org.jetbrains.kotlin.utils.KotlinNativePaths

class BaseNativeConfig(val configuration: CompilerConfiguration) {
    val distribution = run {
        val overridenProperties = mutableMapOf<String, String>().apply {
            configuration.get(KonanConfigKeys.OVERRIDE_KONAN_PROPERTIES)?.let(this::putAll)
            configuration.get(KonanConfigKeys.LLVM_VARIANT)?.getKonanPropertiesEntry()?.let { (key, value) ->
                put(key, value)
            }
        }

        Distribution(
                configuration.get(KonanConfigKeys.KONAN_HOME) ?: KotlinNativePaths.homePath.absolutePath,
                false,
                configuration.get(KonanConfigKeys.RUNTIME_FILE),
                overridenProperties,
                configuration.get(KonanConfigKeys.KONAN_DATA_DIR)
        )
    }

    val platformManager = PlatformManager(distribution)
    val targetManager = platformManager.targetManager(configuration.get(KonanConfigKeys.TARGET))
    val target = targetManager.target

    val resolve = KonanLibrariesResolveSupport(
            configuration, target, distribution, resolveManifestDependenciesLenient = true
    )
}