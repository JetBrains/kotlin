/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.allopen

import com.intellij.mock.MockProject
import org.jetbrains.kotlin.allopen.AllOpenCommandLineProcessor.Companion.SUPPORTED_PRESETS
import org.jetbrains.kotlin.allopen.AllOpenConfigurationKeys.ANNOTATION
import org.jetbrains.kotlin.allopen.AllOpenConfigurationKeys.PRESET
import org.jetbrains.kotlin.allopen.fir.FirAllOpenExtensionRegistrar
import org.jetbrains.kotlin.compiler.plugin.*
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.extensions.DeclarationAttributeAltererExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

object AllOpenConfigurationKeys {
    val ANNOTATION: CompilerConfigurationKey<List<String>> = CompilerConfigurationKey.create("annotation qualified name")
    val PRESET: CompilerConfigurationKey<List<String>> = CompilerConfigurationKey.create("annotation preset")
}

class AllOpenCommandLineProcessor : CommandLineProcessor {
    companion object {
        val SUPPORTED_PRESETS = mapOf(
            "spring" to listOf(
                "org.springframework.stereotype.Component",
                "org.springframework.transaction.annotation.Transactional",
                "org.springframework.scheduling.annotation.Async",
                "org.springframework.cache.annotation.Cacheable",
                "org.springframework.boot.test.context.SpringBootTest",
                "org.springframework.validation.annotation.Validated"
            ),
            "quarkus" to listOf(
                "javax.enterprise.context.ApplicationScoped",
                "javax.enterprise.context.RequestScoped"
            ),
            "micronaut" to listOf(
                "io.micronaut.aop.Around",
                "io.micronaut.aop.Introduction",
                "io.micronaut.aop.InterceptorBinding",
                "io.micronaut.aop.InterceptorBindingDefinitions"
            )
        )

        val ANNOTATION_OPTION = CliOption(
            "annotation", "<fqname>", "Annotation qualified names",
            required = false, allowMultipleOccurrences = true
        )

        val PRESET_OPTION = CliOption(
            "preset", "<name>", "Preset name (${SUPPORTED_PRESETS.keys.joinToString()})",
            required = false, allowMultipleOccurrences = true
        )

        const val PLUGIN_ID = "org.jetbrains.kotlin.allopen"
    }

    override val pluginId = PLUGIN_ID
    override val pluginOptions = listOf(ANNOTATION_OPTION, PRESET_OPTION)

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) = when (option) {
        ANNOTATION_OPTION -> configuration.appendList(ANNOTATION, value)
        PRESET_OPTION -> configuration.appendList(PRESET, value)
        else -> throw CliOptionProcessingException("Unknown option: ${option.optionName}")
    }
}

class AllOpenComponentRegistrar : ComponentRegistrar {
    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        val annotations = configuration.get(ANNOTATION)?.toMutableList() ?: mutableListOf()
        configuration.get(PRESET)?.forEach { preset ->
            SUPPORTED_PRESETS[preset]?.let { annotations += it }
        }
        if (annotations.isEmpty()) return

        DeclarationAttributeAltererExtension.registerExtension(project, CliAllOpenDeclarationAttributeAltererExtension(annotations))
        FirExtensionRegistrar.registerExtension(project, FirAllOpenExtensionRegistrar(annotations))
    }

    override val supportsK2: Boolean
        get() = true
}
