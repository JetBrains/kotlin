/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.internal

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.internal.variant.TestVariantData
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.jetbrains.kotlin.gradle.model.builder.KotlinAndroidExtensionModelBuilder
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.utils.*
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

        project.logger.error(
            "Error: The 'kotlin-android-extensions' Gradle plugin is no longer supported. " +
                    "Please use this migration guide (https://goo.gle/kotlin-android-extensions-deprecation) to start " +
                    "working with View Binding (https://developer.android.com/topic/libraries/view-binding) " +
                    "and the 'kotlin-parcelize' plugin."
        )
    }

    private fun addAndroidExtensionsRuntime(project: Project) {
        val kotlinPluginVersion = project.getKotlinPluginVersion()
        project.configurations.matching { it.name == "implementation" }.all { configuration ->
            configuration.dependencies.add(
                project.dependencies.create(
                    "org.jetbrains.kotlin:kotlin-android-extensions-runtime:$kotlinPluginVersion"
                )
            )
        }
    }
}

class AndroidSubplugin : KotlinCompilerPluginSupportPlugin {
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
            @Suppress("UNCHECKED_CAST")
            return applyExperimental(
                kotlinCompilation.compileTaskProvider as TaskProvider<KotlinCompile>,
                androidExtension,
                androidExtensionsExtension,
                project,
                kotlinCompilation.androidVariant
            )
        }

        val sourceSets = androidExtension.sourceSets

        val pluginOptions = arrayListOf<SubpluginOption>()
        pluginOptions += SubpluginOption(
            "features",
            AndroidExtensionsFeature.parseFeatures(androidExtensionsExtension.features).joinToString(",") { it.featureName }
        )

        val mainSourceSet = sourceSets.getByName("main")
        val manifestFile = mainSourceSet.manifest.srcFile
        val applicationPackage = getApplicationPackage(androidExtension, manifestFile) ?: run {
            project.logger.warn(
                "Application package name is not present in the manifest file (${manifestFile.absolutePath})"
            )
            ""
        }
        pluginOptions += SubpluginOption("package", applicationPackage)

        fun addVariant(@Suppress("TYPEALIAS_EXPANSION_DEPRECATION") sourceSet: DeprecatedAndroidSourceSet) {
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
            @Suppress("UNCHECKED_CAST")
            (kotlinCompilation.compileTaskProvider as TaskProvider<KotlinCompile>).configure {
                it.androidLayoutResourceFiles.from(
                    sourceSet.res.getSourceDirectoryTrees().layoutDirectories
                )
            }
        }

        addVariant(mainSourceSet)

        androidExtension.productFlavors.configureEach { flavor ->
            androidExtension.sourceSets.findByName(flavor.name)?.let {
                addVariant(it)
            }
        }

        return project.provider { wrapPluginOptions(pluginOptions, "configuration") }
    }

    private val List<ConfigurableFileTree>.layoutDirectories
        get() = map { tree ->
            tree.matching {
                it.include("**/layout/**")
                it.include("**/layout-*/**")
            }
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
        pluginOptions += SubpluginOption("package", getApplicationPackage(androidExtension, project, mainSourceSet))

        fun addVariant(name: String, resDirectories: List<ConfigurableFileTree>) {
            val optionValue = lazy {
                buildString {
                    append(name)
                    append(';')
                    resDirectories.map { it.dir }.joinTo(this, separator = ";") { it.normalize().absolutePath }
                }
            }
            pluginOptions += CompositeSubpluginOption(
                "variant", optionValue, listOf(
                    SubpluginOption("variantName", name),
                    // use INTERNAL option kind since the resources are tracked as sources (see below)
                    FilesSubpluginOption(
                        "resDirs",
                        resDirectories.map { it.dir }
                    )
                )
            )

            kotlinCompile.configure {
                it.androidLayoutResourceFiles.from(resDirectories.layoutDirectories)
            }
        }

        fun addSourceSetAsVariant(name: String) {
            val sourceSet = androidExtension.sourceSets.findByName(name) ?: return
            val srcDirs = sourceSet.res.getSourceDirectoryTrees()
            if (srcDirs.isNotEmpty()) {
                addVariant(sourceSet.name, srcDirs)
            }
        }

        addSourceSetAsVariant("main")

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

    @Suppress("TYPEALIAS_EXPANSION_DEPRECATION")
    private fun getVariantComponentNames(flavorData: Any?): VariantComponentNames? = when (flavorData) {
        is TestVariantData -> getVariantComponentNames(flavorData.testedVariantData)
        is DeprecatedAndroidTestVariant -> getVariantComponentNames(flavorData.testedVariant)
        is DeprecatedAndroidBaseVariant -> VariantComponentNames(flavorData.name, flavorData.flavorName, flavorData.buildType.name)
        else -> null
    }

    private data class VariantComponentNames(val variantName: String, val flavorName: String, val buildTypeName: String)

    private fun getApplicationPackage(
        androidExtension: BaseExtension,
        project: Project,
        @Suppress("TYPEALIAS_EXPANSION_DEPRECATION") mainSourceSet: DeprecatedAndroidSourceSet
    ): String {
        val manifestFile = mainSourceSet.manifest.srcFile
        val applicationPackage = getApplicationPackage(androidExtension, manifestFile)

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

    private fun getApplicationPackage(androidExtension: BaseExtension, manifestFile: File): String? {
        // Starting AGP 7 the package can be set via the DSL namespace value:
        //
        // android {
        //   namespace "com.example"
        // }
        //
        // instead of via the "package" attribute in the manifest file.
        //
        // Starting AGP 8, the package *must* be set via the DSL and the manifest file
        // attribute cannot be used.
        //
        // See https://issuetracker.google.com/issues/172361895
        //
        // Therefore, we try to get the package from there first. Since we support AGP versions
        // prior to AGP 7, we need to reflectively find and call it.
        try {
            val method = androidExtension.javaClass.getDeclaredMethod("getNamespace")
            val result = method.invoke(androidExtension)
            if (result is String && result.isNotEmpty()) {
                return result
            }
        } catch (e: ReflectiveOperationException) {
            // Ignore and try parsing manifest.
        }

        // Didn't find the namespace getter, or it was not set. Try parsing the
        // manifest to find the "package" attribute from there.
        return try {
            manifestFile.parseXml().documentElement.getAttribute("package")
        } catch (e: Exception) {
            null
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
