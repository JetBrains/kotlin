/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.allopen

import com.intellij.mock.MockProject
import org.jetbrains.kotlin.allopen.AllOpenCommandLineProcessor.Companion.SUPPORTED_PRESETS
import org.jetbrains.kotlin.allopen.AllOpenConfigurationKeys.ANNOTATION
import org.jetbrains.kotlin.allopen.AllOpenConfigurationKeys.PRESET
import org.jetbrains.kotlin.compiler.plugin.*
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.extensions.DeclarationAttributeAltererExtension

object AllOpenConfigurationKeys {
    val ANNOTATION: CompilerConfigurationKey<List<String>> =
            CompilerConfigurationKey.create("annotation qualified name")

    val PRESET: CompilerConfigurationKey<List<String>> = CompilerConfigurationKey.create("annotation preset")
}

class AllOpenCommandLineProcessor : CommandLineProcessor {
    companion object {
        val SUPPORTED_PRESETS = mapOf("spring" to listOf(
                "org.springframework.stereotype.Component",
                "org.springframework.transaction.annotation.Transactional",
                "org.springframework.scheduling.annotation.Async",
                "org.springframework.cache.annotation.Cacheable",
                "org.springframework.boot.test.context.SpringBootTest",
                "org.springframework.validation.annotation.Validated"))

        val ANNOTATION_OPTION = CliOption("annotation", "<fqname>", "Annotation qualified names",
                                          required = false, allowMultipleOccurrences = true)

        val PRESET_OPTION = CliOption("preset", "<name>", "Preset name (${SUPPORTED_PRESETS.keys.joinToString()})",
                                      required = false, allowMultipleOccurrences = true)

        val PLUGIN_ID = "org.jetbrains.kotlin.allopen"
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
    }
}