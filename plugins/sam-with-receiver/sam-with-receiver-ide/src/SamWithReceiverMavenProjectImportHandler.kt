/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
package org.jetbrains.kotlin.samWithReceiver.ide

/*
import org.jetbrains.kotlin.annotation.plugin.ide.AbstractMavenImportHandler
import org.jetbrains.kotlin.samWithReceiver.SamWithReceiverCommandLineProcessor
import org.jetbrains.kotlin.utils.PathUtil

class SamWithReceiverMavenProjectImportHandler : AbstractMavenImportHandler() {
    private companion object {
        val ANNOTATION_PARAMETER_PREFIX = "sam-with-receiver:${SamWithReceiverCommandLineProcessor.ANNOTATION_OPTION.name}="
    }

    override val compilerPluginId = SamWithReceiverCommandLineProcessor.PLUGIN_ID
    override val pluginName = "samWithReceiver"
    override val annotationOptionName = SamWithReceiverCommandLineProcessor.ANNOTATION_OPTION.name
    override val mavenPluginArtifactName = "kotlin-maven-sam-with-receiver"
    override val pluginJarFileFromIdea = PathUtil.getKotlinPathsForIdeaPlugin().allOpenPluginJarPath

    override fun getAnnotations(enabledCompilerPlugins: List<String>, compilerPluginOptions: List<String>): List<String>? {
        if ("sam-with-receiver" !in enabledCompilerPlugins) {
            return null
        }

        val annotations = mutableListOf<String>()

        for ((presetName, presetAnnotations) in SamWithReceiverCommandLineProcessor.SUPPORTED_PRESETS) {
            if (presetName in enabledCompilerPlugins) {
                annotations.addAll(presetAnnotations)
            }
        }

        annotations.addAll(compilerPluginOptions.mapNotNull { text ->
            if (!text.startsWith(ANNOTATION_PARAMETER_PREFIX)) return@mapNotNull null
            text.substring(ANNOTATION_PARAMETER_PREFIX.length)
        })

        return annotations
    }
}
*/