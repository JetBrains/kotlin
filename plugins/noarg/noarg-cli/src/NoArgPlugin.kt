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

package org.jetbrains.kotlin.noarg

import com.intellij.mock.MockProject
import org.jetbrains.kotlin.codegen.extensions.ClassBuilderInterceptorExtension
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CliOptionProcessingException
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey

object NoArgConfigurationKeys {
    val ANNOTATION: CompilerConfigurationKey<List<String>> =
            CompilerConfigurationKey.create("annotation qualified name")
}

class NoArgCommandLineProcessor : CommandLineProcessor {
    companion object {
        val PLUGIN_ID = "org.jetbrains.kotlin.noarg"

        val ANNOTATION_OPTION = CliOption("annotation", "<fqname>", "Annotation qualified names",
                                          required = false, allowMultipleOccurrences = true)
    }

    override val pluginId = "org.jetbrains.kotlin.noarg"
    override val pluginOptions = listOf(ANNOTATION_OPTION)

    override fun processOption(option: CliOption, value: String, configuration: CompilerConfiguration) = when (option) {
        ANNOTATION_OPTION -> {
            val paths = configuration.getList(NoArgConfigurationKeys.ANNOTATION).toMutableList()
            paths.add(value)
            configuration.put(NoArgConfigurationKeys.ANNOTATION, paths)
        }
        else -> throw CliOptionProcessingException("Unknown option: ${option.name}")
    }
}

class NoArgComponentRegistrar : ComponentRegistrar {
    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        val annotations = configuration.get(NoArgConfigurationKeys.ANNOTATION) ?: return
        if (annotations.isEmpty()) return

        ClassBuilderInterceptorExtension.registerExtension(project, CliNoArgClassBuilderInterceptorExtension(annotations))
    }
}