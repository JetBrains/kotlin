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

package org.jetbrains.kotlin.annotation.plugin.ide

import org.jdom.Element
import org.jdom.Text
import org.jetbrains.idea.maven.project.MavenProject
import org.jetbrains.kotlin.annotation.plugin.ide.AnnotationBasedCompilerPluginSetup.PluginOption
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.jetbrains.kotlin.idea.maven.MavenProjectImportHandler
import org.jetbrains.kotlin.idea.maven.KotlinMavenImporter.Companion.KOTLIN_PLUGIN_GROUP_ID
import org.jetbrains.kotlin.idea.maven.KotlinMavenImporter.Companion.KOTLIN_PLUGIN_ARTIFACT_ID
import java.io.File

abstract class AbstractMavenImportHandler : MavenProjectImportHandler {
    abstract val compilerPluginId: String
    abstract val pluginName: String
    abstract val mavenPluginArtifactName: String
    abstract val pluginJarFileFromIdea: File

    override fun invoke(facet: KotlinFacet, mavenProject: MavenProject) {
        modifyCompilerArgumentsForPlugin(facet, getPluginSetup(mavenProject),
                                         compilerPluginId = compilerPluginId,
                                         pluginName = pluginName)
    }

    abstract fun getOptions(enabledCompilerPlugins: List<String>, compilerPluginOptions: List<String>): List<PluginOption>?

    private fun getPluginSetup(mavenProject: MavenProject): AnnotationBasedCompilerPluginSetup? {
        val kotlinPlugin = mavenProject.plugins.firstOrNull {
            it.groupId == KOTLIN_PLUGIN_GROUP_ID && it.artifactId == KOTLIN_PLUGIN_ARTIFACT_ID
        } ?: return null

        val configuration = kotlinPlugin.configurationElement ?: return null

        val enabledCompilerPlugins = configuration.getElement("compilerPlugins")
                ?.getElements()
                ?.flatMap { plugin -> plugin.content.mapNotNull { (it as? Text)?.text } }
                ?: emptyList()

        val compilerPluginOptions = configuration.getElement("pluginOptions")
                ?.getElements()
                ?.flatMap { it.content }
                ?.mapTo(mutableListOf()) { (it as Text).text }
                ?: mutableListOf<String>()

        // We can't use the plugin from Gradle as it may have the incompatible version
        val classpath = listOf(pluginJarFileFromIdea.absolutePath)

        val options = getOptions(enabledCompilerPlugins, compilerPluginOptions) ?: return null
        return AnnotationBasedCompilerPluginSetup(options, classpath)
    }

    private fun Element.getElement(name: String) = content.firstOrNull { it is Element && it.name == name } as? Element

    @Suppress("UNCHECKED_CAST")
    private fun Element.getElements() = content.filterIsInstance<Element>()
}