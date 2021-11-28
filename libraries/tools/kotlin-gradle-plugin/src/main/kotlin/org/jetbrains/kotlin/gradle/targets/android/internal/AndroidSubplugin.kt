/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.internal

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.api.AndroidSourceSet
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.TestVariant
import com.android.build.gradle.internal.variant.TestVariantData
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.model.builder.KotlinAndroidExtensionModelBuilder
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.w3c.dom.Document
import java.io.File
import java.util.concurrent.Callable
import javax.inject.Inject
import javax.xml.parsers.DocumentBuilderFactory

// Use apply plugin: 'kotlin-android-extensions' to enable Android Extensions in an Android project.
class AndroidExtensionsSubpluginIndicator @Inject internal constructor(private val registry: ToolingModelBuilderRegistry) :
    Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.create("androidExtensions", AndroidExtensionsExtension::class.java)
        addAndroidExtensionsRuntime(project)
        registry.register(KotlinAndroidExtensionModelBuilder())
        project.plugins.apply(AndroidSubplugin::class.java)

        project.logger.warn(
            "Warning: The 'kotlin-android-extensions' Gradle plugin is deprecated. " +
                    "Please use this migration guide (https://goo.gle/kotlin-android-extensions-deprecation) to start " +
                    "working with View Binding (https://developer.android.com/topic/libraries/view-binding) " +
                    "and the 'kotlin-parcelize' plugin."
        )
    }

    private fun addAndroidExtensionsRuntime(project: Project) {
        val kotlinPluginVersion = project.getKotlinPluginVersion()

        project.configurations.all { configuration ->
            val name = configuration.name
            if (name != "implementation" && name != "compile") return@all

            androidPluginVersion ?: return@all
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

class AndroidSubplugin :
    KotlinCompilerPluginSupportPlugin,
    @Suppress("DEPRECATION_ERROR") // implementing to fix KT-39809
    KotlinGradleSubplugin<AbstractCompile>
{
    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {
        if (kotlinCompilation !is KotlinJvmAndroidCompilation)
            return false

        val project = kotlinCompilation.target.project

        if (project.extensions.findByName("android") !is BaseExtension)
            return false

        if (project.plugins.findPlugin(AndroidExtensionsSubpluginIndicator::class.java) == null)
            return false

        return true
    }

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>
    ): Provider<List<SubpluginOption>> {
        kotlinCompilation as KotlinJvmAndroidCompilation
        val project = kotlinCompilation.target.project

        val androidExtension = project.extensions.getByName("android") as BaseExtension
        val androidExtensionsExtension = project.extensions.getByType(AndroidExtensionsExtension::class.java)

        if (androidExtensionsExtension.isExperimental) {
            return applyExperimental(
                kotlinCompilation.compileKotlinTaskProvider, androidExtension, androidExtensionsExtension,
                project, kotlinCompilation.androidVariant
            )
        }

        val sourceSets = androidExtension.sourceSets

        val pluginOptions = arrayListOf<SubpluginOption>()
        pluginOptions += SubpluginOption("features",
                                         AndroidExtensionsFeature.parseFeatures(androidExtensionsExtension.features).joinToString(",") { it.featureName })

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
            val optionValue = lazy {
                sourceSet.name + ';' + sourceSet.res.srcDirs.joinToString(";") { it.absolutePath }
            }
            pluginOptions += CompositeSubpluginOption(
                "variant", optionValue, listOf(
                    SubpluginOption("sourceSetName", sourceSet.name),
                    //use the INTERNAL option kind since the resources are tracked as sources (see below)
                    FilesSubpluginOption("resDirs", project.files(Callable { sourceSet.res.srcDirs }))
                )
            )
            kotlinCompilation.compileKotlinTaskProvider.configure {
                it.source(getLayoutDirectories(project, sourceSet.res.srcDirs))
            }
        }

        addVariant(mainSourceSet)

        val flavorSourceSets = androidExtension.productFlavors
            .mapNotNull { androidExtension.sourceSets.findByName(it.name) }

        for (sourceSet in flavorSourceSets) {
            addVariant(sourceSet)
        }

        return project.provider { wrapPluginOptions(pluginOptions, "configuration") }
    }

    private fun getLayoutDirectories(project: Project, resDirectories: Iterable<File>): FileCollection {
        fun isLayoutDirectory(file: File) = file.name == "layout" || file.name.startsWith("layout-")

        return project.files(Callable {
            resDirectories.flatMap { resDir ->
                (resDir.listFiles(::isLayoutDirectory)).orEmpty().asList()
            }
        })
    }

    private fun applyExperimental(
        kotlinCompile: TaskProvider<out KotlinCompile>,
        androidExtension: BaseExtension,
        androidExtensionsExtension: AndroidExtensionsExtension,
        project: Project,
        variantData: Any?
    ): Provider<List<SubpluginOption>> {
        val pluginOptions = arrayListOf<SubpluginOption>()
        pluginOptions += SubpluginOption(
            "features",
            AndroidExtensionsFeature.parseFeatures(androidExtensionsExtension.features).joinToString(",") { it.featureName }
        )

        pluginOptions += SubpluginOption("experimental", "true")
        pluginOptions += SubpluginOption(
            "defaultCacheImplementation",
            androidExtensionsExtension.defaultCacheImplementation.optionName
        )

        val mainSourceSet = androidExtension.sourceSets.getByName("main")
        pluginOptions += SubpluginOption("package", getApplicationPackage(project, mainSourceSet))

        fun addVariant(name: String, resDirectories: FileCollection) {
            val optionValue = lazy {
                buildString {
                    append(name)
                    append(';')
                    resDirectories.joinTo(this, separator = ";") { it.canonicalPath }
                }
            }
            pluginOptions += CompositeSubpluginOption(
                "variant", optionValue, listOf(
                    SubpluginOption("variantName", name),
                    // use INTERNAL option kind since the resources are tracked as sources (see below)
                    FilesSubpluginOption("resDirs", resDirectories)
                )
            )

            kotlinCompile.configure {
                it.inputs.files(getLayoutDirectories(project, resDirectories)).withPathSensitivity(PathSensitivity.RELATIVE)
            }
        }

        fun addSourceSetAsVariant(name: String) {
            val sourceSet = androidExtension.sourceSets.findByName(name) ?: return
            val srcDirs = sourceSet.res.srcDirs.toList()
            if (srcDirs.isNotEmpty()) {
                addVariant(sourceSet.name, project.files(srcDirs))
            }
        }

        val resDirectoriesForAllVariants = mutableListOf<FileCollection>()

        forEachVariant(project) { variant ->
            if (getTestedVariantData(variant) != null) return@forEachVariant
            resDirectoriesForAllVariants += variant.getResDirectories()
        }

        val commonResDirectories = getCommonResDirectories(project, resDirectoriesForAllVariants)

        addVariant("main", commonResDirectories)

        getVariantComponentNames(variantData)?.let { (variantName, flavorName, buildTypeName) ->
            addSourceSetAsVariant(buildTypeName)

            if (flavorName.isNotEmpty()) {
                addSourceSetAsVariant(flavorName)
            }

            if (buildTypeName != variantName && buildTypeName != flavorName) {
                addSourceSetAsVariant(variantName)
            }
        }

        return project.provider { wrapPluginOptions(pluginOptions, "configuration") }
    }

    private fun getVariantComponentNames(flavorData: Any?): VariantComponentNames? = when (flavorData) {
        is TestVariantData -> getVariantComponentNames(flavorData.testedVariantData)
        is TestVariant -> getVariantComponentNames(flavorData.testedVariant)
        is BaseVariant -> VariantComponentNames(flavorData.name, flavorData.flavorName, flavorData.buildType.name)
        else -> null
    }

    private data class VariantComponentNames(val variantName: String, val flavorName: String, val buildTypeName: String)

    private fun getCommonResDirectories(project: Project, resDirectories: List<FileCollection>): FileCollection {
        val lazyFiles = lazy {
            if (resDirectories.isEmpty()) {
                emptySet<File>()
            } else {
                resDirectories.first().toMutableSet().apply {
                    resDirectories.drop(1).forEach { retainAll(it) }
                }
            }
        }
        return project.files(Callable { lazyFiles.value })
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

    //region Stub implementation for legacy API, KT-39809
    override fun isApplicable(project: Project, task: AbstractCompile): Boolean = false

    override fun apply(
        project: Project, kotlinCompile: AbstractCompile, javaCompile: AbstractCompile?, variantData: Any?, androidProjectHandler: Any?,
        kotlinCompilation: KotlinCompilation<KotlinCommonOptions>?
    ): List<SubpluginOption> = emptyList()
    //endregion
}