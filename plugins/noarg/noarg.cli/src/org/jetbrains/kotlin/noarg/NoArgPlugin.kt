/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.noarg

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.*
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.noarg.NoArgConfigurationKeys.NOARG_ANNOTATION
import org.jetbrains.kotlin.noarg.NoArgConfigurationKeys.NOARG_INVOKE_INITIALIZERS
import org.jetbrains.kotlin.noarg.NoArgConfigurationKeys.NOARG_PRESET
import org.jetbrains.kotlin.noarg.NoArgPluginNames.ANNOTATION_OPTION_NAME
import org.jetbrains.kotlin.noarg.NoArgPluginNames.INVOKE_INITIALIZERS_OPTION_NAME
import org.jetbrains.kotlin.noarg.NoArgPluginNames.PLUGIN_ID
import org.jetbrains.kotlin.noarg.NoArgPluginNames.SUPPORTED_PRESETS
import org.jetbrains.kotlin.noarg.fir.FirNoArgExtensionRegistrar

object NoArgConfigurationKeys {
    val NOARG_ANNOTATION: CompilerConfigurationKey<List<String>> = CompilerConfigurationKey.create("NOARG_ANNOTATION")
    val NOARG_PRESET: CompilerConfigurationKey<List<String>> = CompilerConfigurationKey.create("NOARG_PRESET")

    // Invoke instance initializers in a no-arg constructor.
    val NOARG_INVOKE_INITIALIZERS: CompilerConfigurationKey<Boolean> = CompilerConfigurationKey.create("NOARG_INVOKE_INITIALIZERS")
}

class NoArgCommandLineProcessor : CommandLineProcessor {
    companion object {
        val ANNOTATION_OPTION = CliOption(
            ANNOTATION_OPTION_NAME, "<fqname>", "Annotation qualified names",
            required = false, allowMultipleOccurrences = true
        )

        val PRESET_OPTION = CliOption(
            "preset", "<name>", "Preset name (${SUPPORTED_PRESETS.keys.joinToString()})",
            required = false, allowMultipleOccurrences = true
        )

        val INVOKE_INITIALIZERS_OPTION = CliOption(
            INVOKE_INITIALIZERS_OPTION_NAME, "true/false",
            "Invoke instance initializers in a no-arg constructor",
            required = false, allowMultipleOccurrences = false
        )
    }

    override val pluginId = PLUGIN_ID
    override val pluginOptions = listOf(ANNOTATION_OPTION, PRESET_OPTION, INVOKE_INITIALIZERS_OPTION)

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) = when (option) {
        ANNOTATION_OPTION -> configuration.appendList(NOARG_ANNOTATION, value)
        PRESET_OPTION -> configuration.appendList(NOARG_PRESET, value)
        INVOKE_INITIALIZERS_OPTION -> configuration.put(NOARG_INVOKE_INITIALIZERS, value == "true")
        else -> throw CliOptionProcessingException("Unknown option: ${option.optionName}")
    }
}

class NoArgComponentRegistrar : CompilerPluginRegistrar() {
    override val pluginId: String get() = PLUGIN_ID

    override val supportsK2: Boolean
        get() = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val annotations = configuration.get(NOARG_ANNOTATION).orEmpty().toMutableList()
        configuration.get(NOARG_PRESET)?.forEach { preset ->
            SUPPORTED_PRESETS[preset]?.let { annotations += it }
        }
        if (annotations.isNotEmpty()) {
            registerNoArgComponents(this, annotations, configuration.getBoolean(NOARG_INVOKE_INITIALIZERS))
        }
    }

    companion object {
        fun registerNoArgComponents(
            extensionStorage: ExtensionStorage,
            annotations: List<String>,
            invokeInitializers: Boolean
        ): Unit = with(extensionStorage) {
            FirExtensionRegistrar.registerExtension(FirNoArgExtensionRegistrar(annotations))
            IrGenerationExtension.registerExtension(NoArgConstructorBodyIrGenerationExtension(annotations, invokeInitializers))
        }
    }
}
