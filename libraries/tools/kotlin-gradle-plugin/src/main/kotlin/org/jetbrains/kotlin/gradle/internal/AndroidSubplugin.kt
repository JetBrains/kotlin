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
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.jetbrains.kotlin.gradle.model.builder.KotlinAndroidExtensionModelBuilder
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.multiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.w3c.dom.Document
import java.io.File
import javax.inject.Inject
import javax.xml.parsers.DocumentBuilderFactory

// Use apply plugin: 'kotlin-android-extensions' to enable Android Extensions in an Android project.
class AndroidExtensionsSubpluginIndicator @Inject internal constructor(private val registry: ToolingModelBuilderRegistry) :
    Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("androidExtensions", AndroidExtensionsExtension::class.java)

        extension.setEvaluatedHandler { evaluatedExtension ->
            if (evaluatedExtension.isExperimental) {
                addAndroidExtensionsRuntimeIfNeeded(project)
            }
        }

        registry.register(KotlinAndroidExtensionModelBuilder())
    }

    private fun addAndroidExtensionsRuntimeIfNeeded(project: Project) {
        val kotlinPluginVersion = project.getKotlinPluginVersion() ?: run {
            project.logger.error("Kotlin plugin should be enabled before 'kotlin-android-extensions'")
            return
        }

        project.configurations.all { configuration ->
            val name = configuration.name
            if (name != "implementation" && name != "compile") return@all

            val androidPluginVersion = loadAndroidPluginVersion() ?: return@all
            val requiredConfigurationName = when {
                compareVersionNumbers(androidPluginVersion, "2.5") > 0 -> "implementation"
                else -> "compile"
            }

            if (name != requiredConfigurationName) return@all

            configuration.dependencies.add(
                project.dependencies.create(
                    "org.jetbrains.kotlin:kotlin-android-extensions-runtime:$kotlinPluginVersion"
                )
            )
        }
    }
}

class AndroidSubplugin : KotlinGradleSubplugin<KotlinCompile> {
    override fun isApplicable(project: Project, task: AbstractCompile): Boolean {
        if (task !is KotlinCompile) return false
        try {
            project.extensions.getByName("android") as? BaseExtension ?: return false
        } catch (e: UnknownDomainObjectException) {
            return false
        }
        if (project.plugins.findPlugin(AndroidExtensionsSubpluginIndicator::class.java) == null) {
            return false
        }
        project.multiplatformExtension?.let { kotlin ->
            return kotlin.targets.any { target ->
                target is KotlinAndroidTarget && target.compilations.any { it.compileKotlinTaskName == task.name }
            }
        }

        return true
    }

    override fun apply(
        project: Project,
        kotlinCompile: KotlinCompile,
        javaCompile: AbstractCompile?,
        variantData: Any?,
        androidProjectHandler: Any?,
        kotlinCompilation: KotlinCompilation<*>?
    ): List<SubpluginOption> {
        val androidExtension = project.extensions.getByName("android") as? BaseExtension ?: return emptyList()
        val androidExtensionsExtension = project.extensions.getByType(AndroidExtensionsExtension::class.java)

        if (androidExtensionsExtension.isExperimental) {
            return applyExperimental(
                kotlinCompile, androidExtension, androidExtensionsExtension,
                project, variantData, androidProjectHandler
            )
        }

        val sourceSets = androidExtension.sourceSets

        val pluginOptions = arrayListOf<SubpluginOption>()
        pluginOptions += SubpluginOption("features", AndroidExtensionsFeature.VIEWS.featureName)

        val mainSourceSet = sourceSets.getByName("main")
        val manifestFile = mainSourceSet.manifest.srcFile
        val applicationPackage = getApplicationPackageFromManifest(manifestFile) ?: run {
            project.logger.warn(
                "Application package name is not present in the manifest file (${manifestFile.absolutePath})"
            )
            ""
        }
        pluginOptions += SubpluginOption("package", applicationPackage)

        fun addVariant(sourceSet: AndroidSourceSet) {
            val optionValue = sourceSet.name + ';' +
                    sourceSet.res.srcDirs.joinToString(";") { it.absolutePath }
            pluginOptions += CompositeSubpluginOption(
                "variant", optionValue, listOf(
                    SubpluginOption("sourceSetName", sourceSet.name),
                    //use the INTERNAL option kind since the resources are tracked as sources (see below)
                    FilesSubpluginOption("resDirs", sourceSet.res.srcDirs.toList())
                )
            )
            kotlinCompile.source(project.files(getLayoutDirectories(sourceSet.res.srcDirs)))
        }

        addVariant(mainSourceSet)

        val flavorSourceSets = androidExtension.productFlavors
            .mapNotNull { androidExtension.sourceSets.findByName(it.name) }

        for (sourceSet in flavorSourceSets) {
            addVariant(sourceSet)
        }

        return wrapPluginOptions(pluginOptions, "configuration")
    }

    private fun getLayoutDirectories(resDirectories: Collection<File>): List<File> {
        fun isLayoutDirectory(file: File) = file.name == "layout" || file.name.startsWith("layout-")

        return resDirectories.flatMap { resDir ->
            (resDir.listFiles(::isLayoutDirectory)).orEmpty().asList()
        }
    }

    private fun applyExperimental(
        kotlinCompile: KotlinCompile,
        androidExtension: BaseExtension,
        androidExtensionsExtension: AndroidExtensionsExtension,
        project: Project,
        variantData: Any?,
        androidProjectHandler: Any?
    ): List<SubpluginOption> {
        @Suppress("UNCHECKED_CAST")
        androidProjectHandler as? AbstractAndroidProjectHandler<Any?> ?: return emptyList()

        val pluginOptions = arrayListOf<SubpluginOption>()
        pluginOptions += SubpluginOption("features",
                                         AndroidExtensionsFeature.parseFeatures(androidExtensionsExtension.features).joinToString(",") { it.featureName })

        pluginOptions += SubpluginOption("experimental", "true")
        pluginOptions += SubpluginOption(
            "defaultCacheImplementation",
            androidExtensionsExtension.defaultCacheImplementation.optionName
        )

        val mainSourceSet = androidExtension.sourceSets.getByName("main")
        pluginOptions += SubpluginOption("package", getApplicationPackage(project, mainSourceSet))

        fun addVariant(name: String, resDirectories: List<File>) {
            val optionValue = buildString {
                append(name)
                append(';')
                resDirectories.joinTo(this, separator = ";") { it.canonicalPath }
            }
            pluginOptions += CompositeSubpluginOption(
                "variant", optionValue, listOf(
                    SubpluginOption("variantName", name),
                    // use INTERNAL option kind since the resources are tracked as sources (see below)
                    FilesSubpluginOption("resDirs", resDirectories)
                )
            )

            kotlinCompile.source(project.files(getLayoutDirectories(resDirectories)))
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

            if (buildTypeName != variantName && buildTypeName != flavorName) {
                addSourceSetAsVariant(variantName)
            }
        }

        return wrapPluginOptions(pluginOptions, "configuration")
    }

    // Android25ProjectHandler.KaptVariant actually contains BaseVariant, not BaseVariantData
    private fun getVariantComponentNames(flavorData: Any?): VariantComponentNames? = when (flavorData) {
        is KaptVariantData<*> -> getVariantComponentNames(flavorData.variantData)
        is TestVariantData -> getVariantComponentNames(flavorData.testedVariantData)
        is TestVariant -> getVariantComponentNames(flavorData.testedVariant)
        is BaseVariant -> VariantComponentNames(flavorData.name, flavorData.flavorName, flavorData.buildType.name)
        is BaseVariantData<*> -> VariantComponentNames(
            flavorData.name, flavorData.variantConfiguration.flavorName,
            flavorData.variantConfiguration.buildType.name
        )
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
            project.logger.warn(
                "Application package name is not present in the manifest file " +
                        "(${manifestFile.absolutePath})"
            )

            return ""
        } else {
            return applicationPackage
        }
    }

    private fun getApplicationPackageFromManifest(manifestFile: File): String? {
        try {
            return manifestFile.parseXml().documentElement.getAttribute("package")
        } catch (e: Exception) {
            return null
        }
    }

    override fun getCompilerPluginId() = "org.jetbrains.kotlin.android"

    override fun getPluginArtifact(): SubpluginArtifact =
        JetBrainsSubpluginArtifact(artifactId = "kotlin-android-extensions")

    private fun File.parseXml(): Document {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        return builder.parse(this)
    }
}