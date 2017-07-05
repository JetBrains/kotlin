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

package org.jetbrains.kotlin.noarg.ide

import org.jetbrains.kotlin.annotation.plugin.ide.AbstractMavenImportHandler
import org.jetbrains.kotlin.noarg.NoArgCommandLineProcessor
import org.jetbrains.kotlin.utils.PathUtil

class NoArgMavenProjectImportHandler : AbstractMavenImportHandler() {
    private companion object {
        val ANNOTATATION_PARAMETER_PREFIX = "no-arg:${NoArgCommandLineProcessor.ANNOTATION_OPTION.name}="
    }

    override val compilerPluginId = NoArgCommandLineProcessor.PLUGIN_ID
    override val pluginName = "noarg"
    override val annotationOptionName = NoArgCommandLineProcessor.ANNOTATION_OPTION.name
    override val mavenPluginArtifactName = "kotlin-maven-noarg"
    override val pluginJarFileFromIdea = PathUtil.kotlinPathsForIdeaPlugin.noArgPluginJarPath

    override fun getAnnotations(enabledCompilerPlugins: List<String>, compilerPluginOptions: List<String>): List<String>? {
        if ("no-arg" !in enabledCompilerPlugins && "jpa" !in enabledCompilerPlugins) {
            return null
        }

        val annotations = mutableListOf<String>()
        for ((presetName, presetAnnotations) in NoArgCommandLineProcessor.SUPPORTED_PRESETS) {
            if (presetName in enabledCompilerPlugins) {
                annotations.addAll(presetAnnotations)
            }
        }

        annotations.addAll(compilerPluginOptions.mapNotNull { text ->
            if (!text.startsWith(ANNOTATATION_PARAMETER_PREFIX)) return@mapNotNull null
            text.substring(ANNOTATATION_PARAMETER_PREFIX.length)
        })

        return annotations
    }
}
