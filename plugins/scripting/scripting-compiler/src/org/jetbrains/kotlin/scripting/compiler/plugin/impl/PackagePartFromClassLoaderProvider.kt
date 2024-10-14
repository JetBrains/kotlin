/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import com.intellij.util.SmartList
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.tryLoadModuleMapping
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.load.kotlin.JvmPackagePartProviderBase
import org.jetbrains.kotlin.metadata.jvm.deserialization.ModuleMapping
import org.jetbrains.kotlin.resolve.CompilerDeserializationConfiguration
import kotlin.script.experimental.jvm.util.forAllMatchingFiles

class PackagePartFromClassLoaderProvider(
    classLoader: ClassLoader,
    languageVersionSettings: LanguageVersionSettings,
    messageCollector: MessageCollector
) : JvmPackagePartProviderBase<String>() {
    override val deserializationConfiguration = CompilerDeserializationConfiguration(languageVersionSettings)

    override val loadedModules: MutableList<ModuleMappingInfo<String>> = SmartList()

    init {
        classLoader.forAllMatchingFiles("META-INF/*.${ModuleMapping.MAPPING_FILE_EXT}") { name, stream ->
            tryLoadModuleMapping(
                { stream.readBytes() }, name, name, deserializationConfiguration, messageCollector
            )?.let {
                val moduleName = name.removePrefix("META-INF/").removeSuffix(".${ModuleMapping.MAPPING_FILE_EXT}")
                loadedModules.add(ModuleMappingInfo(name, it, moduleName))
            }
        }
    }
}

