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

package org.jetbrains.kotlin.gradle.internal

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.api.AndroidSourceSet
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.gradle.plugin.KotlinGradleSubplugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import org.jetbrains.kotlin.gradle.plugin.android.AndroidGradleWrapper
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.w3c.dom.Document
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

// Use apply plugin: 'kotlin-android-extensions' to enable Android Extensions in an Android project.
// Just a marker plugin.
class AndroidExtensionsSubpluginIndicator : Plugin<Project> {
    override fun apply(target: Project?) {}
}

class AndroidSubplugin : KotlinGradleSubplugin<KotlinCompile> {
    private companion object {
        @Volatile
        var migrateWarningReported: Boolean = false
    }

    override fun isApplicable(project: Project, task: KotlinCompile): Boolean {
        try {
            project.extensions.getByName("android") as? BaseExtension ?: return false
        } catch (e: UnknownDomainObjectException) {
            return false
        }
        if (project.plugins.findPlugin(AndroidExtensionsSubpluginIndicator::class.java) == null) {
            val dependencies = project.buildscript.configurations.getByName("classpath").dependencies
            if (dependencies.any { it.name == getArtifactName() && it.group == getGroupName() } && !migrateWarningReported) {
                project.logger.warn("To enable Android Extensions, use: \"apply plugin: 'kotlin-android-extensions'\"")
                migrateWarningReported = true
            }
            return false
        }
        return true
    }

    override fun apply(
            project: Project,
            kotlinCompile: KotlinCompile,
            javaCompile: AbstractCompile, 
            variantData: Any?, 
            javaSourceSet: SourceSet?
    ): List<SubpluginOption> {
        val androidExtension = project.extensions.getByName("android") as? BaseExtension ?: return emptyList()
        val sourceSets = androidExtension.sourceSets

        val pluginOptions = arrayListOf<SubpluginOption>()

        val mainSourceSet = sourceSets.getByName("main")
        val manifestFile = mainSourceSet.manifest.srcFile
        val applicationPackage = getApplicationPackageFromManifest(manifestFile) ?: run {
            project.logger.warn(
                    "Application package name is not present in the manifest file (${manifestFile.absolutePath})")
            ""
        }
        pluginOptions += SubpluginOption("package", applicationPackage)

        fun addVariant(sourceSet: AndroidSourceSet) {
            pluginOptions += SubpluginOption("variant", sourceSet.name + ';' +
                    sourceSet.res.srcDirs.joinToString(";") { it.absolutePath })
        }

        addVariant(mainSourceSet)

        val flavorSourceSets = AndroidGradleWrapper.getProductFlavorsSourceSets(androidExtension).filterNotNull()
        for (sourceSet in flavorSourceSets) {
            addVariant(sourceSet)
        }

        return pluginOptions
    }

    private fun getApplicationPackageFromManifest(manifestFile: File): String? {
        try {
            return manifestFile.parseXml().documentElement.getAttribute("package")
        }
        catch (e: Exception) {
            return null
        }
    }

    override fun getCompilerPluginId() = "org.jetbrains.kotlin.android"

    override fun getGroupName() = "org.jetbrains.kotlin"

    override fun getArtifactName() = "kotlin-android-extensions"

    fun File.parseXml(): Document {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        return builder.parse(this)
    }
}