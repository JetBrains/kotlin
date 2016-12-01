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

package org.jetbrains.kotlin.allopen.ide

import org.jetbrains.kotlin.allopen.AllOpenCommandLineProcessor
import org.jetbrains.kotlin.annotation.plugin.ide.AbstractMavenImportHandler

class AllOpenMavenProjectImportHandler : AbstractMavenImportHandler() {
    private companion object {
        val ANNOTATION_PARAMETER_PREFIX = "all-open:${AllOpenCommandLineProcessor.ANNOTATION_OPTION.name}="

        private val SPRING_ALLOPEN_ANNOTATIONS = listOf(
                "org.springframework.stereotype.Component",
                "org.springframework.transaction.annotation.Transactional",
                "org.springframework.scheduling.annotation.Async",
                "org.springframework.cache.annotation.Cacheable"
        )
    }

    override val compilerPluginId = AllOpenCommandLineProcessor.PLUGIN_ID
    override val pluginName = "allopen"
    override val annotationOptionName = AllOpenCommandLineProcessor.ANNOTATION_OPTION.name
    override val mavenPluginArtifactName = "kotlin-maven-allopen"

    override fun getAnnotations(enabledCompilerPlugins: List<String>, compilerPluginOptions: List<String>): List<String>? {
        if ("all-open" !in enabledCompilerPlugins && "spring" !in enabledCompilerPlugins) {
            return null
        }

        val annotations = mutableListOf<String>()
        if ("spring" in enabledCompilerPlugins) {
            annotations.addAll(SPRING_ALLOPEN_ANNOTATIONS)
        }

        annotations.addAll(compilerPluginOptions.mapNotNull { text ->
            if (!text.startsWith(ANNOTATION_PARAMETER_PREFIX)) return@mapNotNull null
            text.substring(ANNOTATION_PARAMETER_PREFIX.length)
        })

        return annotations
    }
}