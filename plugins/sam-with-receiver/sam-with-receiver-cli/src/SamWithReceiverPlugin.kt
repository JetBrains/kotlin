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

package org.jetbrains.kotlin.samWithReceiver

import com.intellij.mock.MockProject
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CliOptionProcessingException
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.load.java.sam.SamWithReceiverResolver
import org.jetbrains.kotlin.samWithReceiver.SamWithReceiverConfigurationKeys.ANNOTATION

object SamWithReceiverConfigurationKeys {
    val ANNOTATION: CompilerConfigurationKey<List<String>> =
            CompilerConfigurationKey.create("annotation qualified name")
}

class SamWithReceiverCommandLineProcessor : CommandLineProcessor {
    companion object {
        val ANNOTATION_OPTION = CliOption("annotation", "<fqname>", "Annotation qualified names",
                                          required = false, allowMultipleOccurrences = true)

        val PLUGIN_ID = "org.jetbrains.kotlin.samWithReceiver"
    }

    override val pluginId = PLUGIN_ID
    override val pluginOptions = listOf(ANNOTATION_OPTION)

    override fun processOption(option: CliOption, value: String, configuration: CompilerConfiguration) = when (option) {
        ANNOTATION_OPTION -> configuration.appendList(ANNOTATION, value)
        else -> throw CliOptionProcessingException("Unknown option: ${option.name}")
    }
}

class SamWithReceiverComponentRegistrar : ComponentRegistrar {
    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        val annotations = configuration.get(SamWithReceiverConfigurationKeys.ANNOTATION) ?: return
        if (annotations.isEmpty()) return

        StorageComponentContainerContributor.registerExtension(project, CliSamWithReceiverComponentContributor(annotations))
    }
}

class CliSamWithReceiverComponentContributor(val annotations: List<String>): StorageComponentContainerContributor {
    override fun onContainerComposed(container: ComponentProvider, moduleInfo: ModuleInfo?) {
        container.get<SamWithReceiverResolver>().registerExtension(SamWithReceiverResolverExtension(annotations))
    }
}