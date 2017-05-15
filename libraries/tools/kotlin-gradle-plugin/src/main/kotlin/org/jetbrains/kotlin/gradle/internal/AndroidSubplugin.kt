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
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.TestVariant
import com.android.build.gradle.internal.variant.BaseVariantData
import com.android.build.gradle.internal.variant.TestVariantData
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.gradle.plugin.KotlinGradleSubplugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
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
    override fun isApplicable(project: Project, task: KotlinCompile): Boolean {
        try {
            project.extensions.getByName("android") as? BaseExtension ?: return false
        } catch (e: UnknownDomainObjectException) {
            return false
        }
        if (project.plugins.findPlugin(AndroidExtensionsSubpluginIndicator::class.java) == null) {
            return false
        }
        return true
    }

    override fun apply(
            project: Project,
            kotlinCompile: KotlinCompile,
            javaCompile: AbstractCompile, 
            variantData: Any?,
            androidProjectHandler: Any?,
            javaSourceSet: SourceSet?
    ): List<SubpluginOption> {
        @Suppress("UNCHECKED_CAST")
        androidProjectHandler as? AbstractAndroidProjectHandler<Any?> ?: return emptyList()

        val androidExtension = project.extensions.getByName("android") as? BaseExtension ?: return emptyList()
        val pluginOptions = arrayListOf<SubpluginOption>()

        val mainSourceSet = androidExtension.sourceSets.getByName("main")
        pluginOptions += SubpluginOption("package", getApplicationPackage(project, mainSourceSet))

        fun addVariant(name: String, resDirectories: List<File>) {
            pluginOptions += SubpluginOption("variant", buildString {
                append(name)
                append(';')
                resDirectories.joinTo(this, separator = ";") { it.canonicalPath }
            })
        }

        fun addSourceSetAsVariant(name: String) {
            val sourceSet = androidExtension.sourceSets.findByName(name) ?: return
            val srcDirs = sourceSet.res.srcDirs.toList()
            if (srcDirs.isNotEmpty()) {
                addVariant(sourceSet.name, srcDirs)
            }
        }

        val resDirectoriesForAllVariants = mutableListOf<List<File>>()

        androidProjectHandler.forEachVariant(project) { variant ->
            if (androidProjectHandler.getTestedVariantData(variant) != null) return@forEachVariant
            resDirectoriesForAllVariants += androidProjectHandler.getResDirectories(variant)
        }

        val commonResDirectories = getCommonResDirectories(resDirectoriesForAllVariants)

        addVariant("main", commonResDirectories.toList())

        getVariantComponentNames(variantData)?.let { (variantName, flavorName, buildTypeName) ->
            addSourceSetAsVariant(buildTypeName)

            if (flavorName.isNotEmpty()) {
                addSourceSetAsVariant(flavorName)
            }

            addSourceSetAsVariant(variantName)
        }

        return pluginOptions
    }

    // Android25ProjectHandler.KaptVariant actually contains BaseVariant, not BaseVariantData
    private fun getVariantComponentNames(flavorData: Any?): VariantComponentNames? = when(flavorData) {
        is KaptVariantData<*> -> getVariantComponentNames(flavorData.variantData)
        is TestVariantData -> getVariantComponentNames(flavorData.testedVariantData)
        is TestVariant -> getVariantComponentNames(flavorData.testedVariant)
        is BaseVariant -> VariantComponentNames(flavorData.name, flavorData.flavorName, flavorData.buildType.name)
        is BaseVariantData<*> -> VariantComponentNames(flavorData.name, flavorData.variantConfiguration.flavorName,
                flavorData.variantConfiguration.buildType.name)
        else -> null
    }

    private data class VariantComponentNames(val variantName: String, val flavorName: String, val buildTypeName: String)

    private fun getCommonResDirectories(resDirectories: List<List<File>>): Set<File> {
        var common = resDirectories.firstOrNull()?.toSet() ?: return emptySet()

        for (resDirs in resDirectories.drop(1)) {
            common = common.intersect(resDirs)
        }

        return common
    }

    private fun getApplicationPackage(project: Project, mainSourceSet: AndroidSourceSet): String {
        val manifestFile = mainSourceSet.manifest.srcFile
        val applicationPackage = getApplicationPackageFromManifest(manifestFile)

        if (applicationPackage == null) {
            project.logger.warn("Application package name is not present in the manifest file " +
                    "(${manifestFile.absolutePath})")

            return ""
        } else {
            return applicationPackage
        }
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

    private fun File.parseXml(): Document {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        return builder.parse(this)
    }
}