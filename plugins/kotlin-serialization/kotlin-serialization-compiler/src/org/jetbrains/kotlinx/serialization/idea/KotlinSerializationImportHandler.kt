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

package org.jetbrains.kotlinx.serialization.idea

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.util.PathUtil
import org.jetbrains.idea.maven.project.MavenProject
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.idea.configuration.GradleProjectImportHandler
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.jetbrains.kotlin.idea.maven.MavenProjectImportHandler
import org.jetbrains.kotlinx.serialization.idea.KotlinSerializationImportHandler.modifyCompilerArguments
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData
import java.io.File

class KotlinSerializationGradleImportHandler : GradleProjectImportHandler {
    override fun importBySourceSet(facet: KotlinFacet, sourceSetNode: DataNode<GradleSourceSetData>) {
        modifyCompilerArguments(facet, PLUGIN_GRADLE_JAR)
    }

    override fun importByModule(facet: KotlinFacet, moduleNode: DataNode<ModuleData>) {
        modifyCompilerArguments(facet, PLUGIN_GRADLE_JAR)
    }

    private val PLUGIN_GRADLE_JAR = "kotlinx-gradle-serialization-plugin"
}

class KotlinSerializationMavenImportHandler: MavenProjectImportHandler {
    override fun invoke(facet: KotlinFacet, mavenProject: MavenProject) {
        modifyCompilerArguments(facet, PLUGIN_MAVEN_JAR)
    }

    private val PLUGIN_MAVEN_JAR = "kotlinx-maven-serialization-plugin"
}

internal object KotlinSerializationImportHandler {
    private val PLUGIN_JPS_JAR :String
        get() = File(PathUtil.getJarPathForClass(this::class.java)).absolutePath

    fun modifyCompilerArguments(facet: KotlinFacet, buildSystemPluginJar: String) {
        val facetSettings = facet.configuration.settings
        val commonArguments = facetSettings.compilerArguments ?: CommonCompilerArguments.DummyImpl()

        val oldPluginClasspaths = (commonArguments.pluginClasspaths ?: emptyArray()).filterTo(mutableListOf()) {
            val lastIndexOfFile = it.lastIndexOfAny(charArrayOf('/', File.separatorChar))
            if (lastIndexOfFile < 0) {
                return@filterTo true
            }
            !it.drop(lastIndexOfFile + 1).matches("$buildSystemPluginJar-.*\\.jar".toRegex())
        }

        val newPluginClasspaths = oldPluginClasspaths + PLUGIN_JPS_JAR
        commonArguments.pluginClasspaths = newPluginClasspaths.toTypedArray()
        facetSettings.compilerArguments = commonArguments
    }
}