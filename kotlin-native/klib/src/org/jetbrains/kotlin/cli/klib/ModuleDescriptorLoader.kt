/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.klib

import org.jetbrains.kotlin.backend.common.serialization.metadata.DynamicTypeDeserializer
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.descriptors.konan.isNativeStdlib
import org.jetbrains.kotlin.konan.library.resolverByName
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.metadata.KlibMetadataFactories
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.utils.KotlinNativePaths

internal class ModuleDescriptorLoader(output: KlibToolOutput) {
    private val logger = KlibToolLogger(output)

    fun load(library: KotlinLibrary): ModuleDescriptorImpl {
        val storageManager = LockBasedStorageManager("klib")

        val module = KlibFactories.DefaultDeserializedDescriptorFactory.createDescriptorAndNewBuiltIns(library, languageVersionSettings, storageManager, null)

        val defaultModules = mutableListOf<ModuleDescriptorImpl>()
        if (!module.isNativeStdlib()) {
            val resolver = resolverByName(
                    emptyList(),
                    distributionKlib = Distribution(KotlinNativePaths.homePath.absolutePath).klib,
                    skipCurrentDir = true,
                    logger = logger
            )
            resolver.defaultLinks(noStdLib = false, noDefaultLibs = true, noEndorsedLibs = true).mapTo(defaultModules) {
                KlibFactories.DefaultDeserializedDescriptorFactory.createDescriptor(it, languageVersionSettings, storageManager, module.builtIns, null)
            }
        }

        (defaultModules + module).let { allModules ->
            allModules.forEach { it.setDependencies(allModules) }
        }

        return module
    }

    companion object {
        val languageVersionSettings = LanguageVersionSettingsImpl(LanguageVersion.LATEST_STABLE, ApiVersion.LATEST_STABLE)

        private val KlibFactories = KlibMetadataFactories(::KonanBuiltIns, DynamicTypeDeserializer)
    }
}
