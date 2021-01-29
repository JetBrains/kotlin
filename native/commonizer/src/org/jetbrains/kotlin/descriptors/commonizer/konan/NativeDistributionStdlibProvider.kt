/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.konan

import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.ModulesProvider
import org.jetbrains.kotlin.descriptors.commonizer.ModulesProvider.ModuleInfo
import org.jetbrains.kotlin.descriptors.commonizer.utils.NativeFactories
import org.jetbrains.kotlin.konan.library.KONAN_STDLIB_NAME
import org.jetbrains.kotlin.storage.StorageManager
import java.io.File

internal class NativeDistributionStdlibProvider(
    private val storageManager: StorageManager,
    private val stdlib: NativeLibrary
) : ModulesProvider {
    private val moduleInfo = ModuleInfo(
        name = KONAN_STDLIB_NAME,
        originalLocation = File(stdlib.library.libraryFile.absolutePath),
        cInteropAttributes = null
    )

    override fun loadModuleInfos(): Map<String, ModuleInfo> = mapOf(KONAN_STDLIB_NAME to moduleInfo)

    override fun loadModules(dependencies: Collection<ModuleDescriptor>): Map<String, ModuleDescriptor> {
        check(dependencies.isEmpty())
        return mapOf(KONAN_STDLIB_NAME to loadStdlibModule())
    }

    private fun loadStdlibModule() =
        NativeFactories.DefaultDeserializedDescriptorFactory.createDescriptorAndNewBuiltIns(
            library = stdlib.library,
            languageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
            storageManager = storageManager,
            packageAccessHandler = null
        ).apply {
            setDependencies(listOf(this))
        }
}