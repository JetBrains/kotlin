/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.container.assignment

import org.jetbrains.kotlin.compiler.plugin.*
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.assignment.ValueContainerAssignmentConfigurationKeys.ANNOTATION
import org.jetbrains.kotlin.container.assignment.ValueContainerAssignmentPluginNames.ANNOTATION_OPTION_NAME
import org.jetbrains.kotlin.container.assignment.ValueContainerAssignmentPluginNames.PLUGIN_ID
import org.jetbrains.kotlin.container.assignment.diagnostics.ValueContainerAssignmentDeclarationChecker
import org.jetbrains.kotlin.container.assignment.k2.FirValueContainerAssignmentExtensionRegistrar
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.extensions.internal.InternalNonStableExtensionPoints
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.resolve.extensions.AssignResolutionAltererExtension

object ValueContainerAssignmentConfigurationKeys {
    val ANNOTATION: CompilerConfigurationKey<List<String>> = CompilerConfigurationKey.create("annotation qualified name")
}

class ValueComponentAssignmentCommandLineProcessor : CommandLineProcessor {
    companion object {
        val ANNOTATION_OPTION = CliOption(
            ANNOTATION_OPTION_NAME, "<fqname>", "Annotation qualified names",
            required = false, allowMultipleOccurrences = true
        )
    }

    override val pluginId = PLUGIN_ID
    override val pluginOptions = listOf(ANNOTATION_OPTION)

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) = when (option) {
        ANNOTATION_OPTION -> configuration.appendList(ANNOTATION, value)
        else -> throw CliOptionProcessingException("Unknown option: ${option.optionName}")
    }
}

class ValueContainerAssignmentComponentRegistrar : CompilerPluginRegistrar() {
    @OptIn(InternalNonStableExtensionPoints::class)
    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val annotations = configuration.get(ANNOTATION)?.toMutableList() ?: mutableListOf()
        if (annotations.isNotEmpty()) {
            AssignResolutionAltererExtension.Companion.registerExtension(ValueContainerAssignResolutionAltererExtension(annotations))
            StorageComponentContainerContributor.registerExtension(ValueContainerAssignmentComponentContainerContributor(annotations))
            FirExtensionRegistrarAdapter.registerExtension(FirValueContainerAssignmentExtensionRegistrar(annotations))
        }
    }

    override val supportsK2: Boolean
        get() = true
}

class ValueContainerAssignmentComponentContainerContributor(private val annotations: List<String>) : StorageComponentContainerContributor {
    override fun registerModuleComponents(
        container: StorageComponentContainer,
        platform: TargetPlatform,
        moduleDescriptor: ModuleDescriptor,
    ) {
        container.useInstance(ValueContainerAssignmentDeclarationChecker(annotations))
    }
}
