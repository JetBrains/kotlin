/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.samWithReceiver

import com.intellij.mock.MockProject
import org.jetbrains.kotlin.compiler.plugin.*
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.samWithReceiver.SamWithReceiverCommandLineProcessor.Companion.SUPPORTED_PRESETS
import org.jetbrains.kotlin.samWithReceiver.SamWithReceiverConfigurationKeys.ANNOTATION
import org.jetbrains.kotlin.samWithReceiver.SamWithReceiverConfigurationKeys.PRESET

object SamWithReceiverConfigurationKeys {
    val ANNOTATION: CompilerConfigurationKey<List<String>> = CompilerConfigurationKey.create("annotation qualified name")

    val PRESET: CompilerConfigurationKey<List<String>> = CompilerConfigurationKey.create("annotation preset")
}

class SamWithReceiverCommandLineProcessor : CommandLineProcessor {
    companion object {
        val SUPPORTED_PRESETS = emptyMap<String, List<String>>()

        val ANNOTATION_OPTION = CliOption(
            "annotation", "<fqname>", "Annotation qualified names",
            required = false, allowMultipleOccurrences = true
        )

        val PRESET_OPTION = CliOption(
            "preset", "<name>", "Preset name (${SUPPORTED_PRESETS.keys.joinToString()})",
            required = false, allowMultipleOccurrences = true
        )

        val PLUGIN_ID = "org.jetbrains.kotlin.samWithReceiver"
    }

    override val pluginId = PLUGIN_ID
    override val pluginOptions = listOf(ANNOTATION_OPTION)

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) = when (option) {
        ANNOTATION_OPTION -> configuration.appendList(ANNOTATION, value)
        PRESET_OPTION -> configuration.appendList(PRESET, value)
        else -> throw CliOptionProcessingException("Unknown option: ${option.optionName}")
    }
}

class SamWithReceiverComponentRegistrar : ComponentRegistrar {
    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        val annotations = configuration.get(ANNOTATION)?.toMutableList() ?: mutableListOf()
        configuration.get(PRESET)?.forEach { preset ->
            SUPPORTED_PRESETS[preset]?.let { annotations += it }
        }
        if (annotations.isEmpty()) return

        StorageComponentContainerContributor.registerExtension(project, CliSamWithReceiverComponentContributor(annotations))
    }
}

class CliSamWithReceiverComponentContributor(val annotations: List<String>) : StorageComponentContainerContributor {
    override fun registerModuleComponents(
        container: StorageComponentContainer,
        platform: TargetPlatform,
        moduleDescriptor: ModuleDescriptor
    ) {
        if (!platform.isJvm()) return

        container.useInstance(SamWithReceiverResolverExtension(annotations))
    }
}
