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

package org.jetbrains.kotlinx.serialization.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinGradleSubplugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import java.io.FileNotFoundException
import java.util.*

class SerializationGradleSubplugin : Plugin<Project> {
    companion object {
        fun isEnabled(project: Project) = project.plugins.findPlugin(SerializationGradleSubplugin::class.java) != null
    }

    override fun apply(project: Project) {
        // nothing here
    }
}

class SerializationKotlinGradleSubplugin : KotlinGradleSubplugin<AbstractCompile> {
    companion object {
        const val SERIALIZATION_GROUP_NAME = "org.jetbrains.kotlinx"
        const val SERIALIZATION_ARTIFACT_NAME = "kotlinx-gradle-serialization-plugin"
        const val SERIALIZATION_ARTIFACT_UNSHADED_NAME = "kotlinx-gradle-serialization-plugin-unshaded"
    }

    private val log = Logging.getLogger(this.javaClass)
    private val pluginVersion = "0.7.0-eap-1"
    private var useUnshaded = false

    override fun isApplicable(project: Project, task: AbstractCompile): Boolean {
        if (!SerializationGradleSubplugin.isEnabled(project)) return false
        // Kotlin/Native task is not an AbstractKotlinCompile and uses unshaded compiler
        if (task !is AbstractKotlinCompile<*>) useUnshaded = true
        return true
    }

    override fun apply(
        project: Project,
        kotlinCompile: AbstractCompile,
        javaCompile: AbstractCompile?,
        variantData: Any?,
        androidProjectHandler: Any?,
        kotlinCompilation: KotlinCompilation?
    )
            : List<SubpluginOption> {
        return emptyList()
    }


    override fun getPluginArtifact(): SubpluginArtifact {
        val artifact = if (useUnshaded) SERIALIZATION_ARTIFACT_UNSHADED_NAME else SERIALIZATION_ARTIFACT_NAME
        return SubpluginArtifact(SERIALIZATION_GROUP_NAME, artifact, pluginVersion)
    }

    override fun getCompilerPluginId() = "org.jetbrains.kotlinx.serialization"
}

// taken from KotlinPluginWrapper.kt
internal fun Logger.kotlinDebug(message: String) {
    this.debug("[KOTLIN] $message")
}

private fun Any.findPluginVersion(log: Logger): String {
    log.kotlinDebug("Loading version information about kotlinx.serialization")
    val props = Properties()
    val propFileName = "plugin.properties"
    val loader = javaClass.classLoader!!
    val inputStream = loader.getResourceAsStream(propFileName)
            ?: throw FileNotFoundException("property file '$propFileName' not found in the classpath")

    props.load(inputStream)

    val pluginVersion = props["plugin.version"] as String
    log.kotlinDebug("Found plugin version [$pluginVersion]")
    return pluginVersion
}