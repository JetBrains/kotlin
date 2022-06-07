/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.noarg

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.compiler.plugin.*
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.noarg.NoArgConfigurationKeys.ANNOTATION
import org.jetbrains.kotlin.noarg.NoArgConfigurationKeys.INVOKE_INITIALIZERS
import org.jetbrains.kotlin.noarg.NoArgConfigurationKeys.PRESET
import org.jetbrains.kotlin.noarg.NoArgPluginNames.ANNOTATION_OPTION_NAME
import org.jetbrains.kotlin.noarg.NoArgPluginNames.INVOKE_INITIALIZERS_OPTION_NAME
import org.jetbrains.kotlin.noarg.NoArgPluginNames.PLUGIN_ID
import org.jetbrains.kotlin.noarg.NoArgPluginNames.SUPPORTED_PRESETS
import org.jetbrains.kotlin.noarg.diagnostic.CliNoArgDeclarationChecker
import org.jetbrains.kotlin.noarg.fir.FirNoArgExtensionRegistrar
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.isJvm

object NoArgConfigurationKeys {
    val ANNOTATION: CompilerConfigurationKey<List<String>> =
        CompilerConfigurationKey.create("annotation qualified name")

    val PRESET: CompilerConfigurationKey<List<String>> = CompilerConfigurationKey.create("annotation preset")

    val INVOKE_INITIALIZERS: CompilerConfigurationKey<Boolean> = CompilerConfigurationKey.create(
        "invoke instance initializers in a no-arg constructor"
    )
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
        ANNOTATION_OPTION -> configuration.appendList(ANNOTATION, value)
        PRESET_OPTION -> configuration.appendList(PRESET, value)
        INVOKE_INITIALIZERS_OPTION -> configuration.put(INVOKE_INITIALIZERS, value == "true")
        else -> throw CliOptionProcessingException("Unknown option: ${option.optionName}")
    }
}

class NoArgComponentRegistrar : CompilerPluginRegistrar() {
    override val supportsK2: Boolean
        get() = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val annotations = configuration.get(ANNOTATION).orEmpty().toMutableList()
        configuration.get(PRESET)?.forEach { preset ->
            SUPPORTED_PRESETS[preset]?.let { annotations += it }
        }
        if (annotations.isNotEmpty()) {
            registerNoArgComponents(
                this,
                annotations,
                configuration.getBoolean(JVMConfigurationKeys.IR),
                configuration.getBoolean(INVOKE_INITIALIZERS),
            )
        }
    }

    companion object {
        fun registerNoArgComponents(
            extensionStorage: ExtensionStorage,
            annotations: List<String>,
            useIr: Boolean,
            invokeInitializers: Boolean
        ) = with(extensionStorage) {
            StorageComponentContainerContributor.registerExtension(CliNoArgComponentContainerContributor(annotations, useIr))
            FirExtensionRegistrarAdapter.registerExtension(FirNoArgExtensionRegistrar(annotations))
            ExpressionCodegenExtension.registerExtension(CliNoArgExpressionCodegenExtension(annotations, invokeInitializers))
            IrGenerationExtension.registerExtension(NoArgIrGenerationExtension(annotations, invokeInitializers))
        }
    }
}

private class CliNoArgComponentContainerContributor(
    private val annotations: List<String>,
    private val useIr: Boolean,
) : StorageComponentContainerContributor {
    override fun registerModuleComponents(
        container: StorageComponentContainer, platform: TargetPlatform, moduleDescriptor: ModuleDescriptor
    ) {
        if (!platform.isJvm()) return

        container.useInstance(CliNoArgDeclarationChecker(annotations, useIr))
    }
}
