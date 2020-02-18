/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.BasePlugin
import com.android.build.gradle.api.*
import com.android.build.gradle.tasks.MergeResources
import com.android.builder.model.SourceProvider
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.gradle.internal.Kapt3GradleSubplugin
import org.jetbrains.kotlin.gradle.internal.KaptTask
import org.jetbrains.kotlin.gradle.internal.KaptVariantData
import org.jetbrains.kotlin.gradle.plugin.android.AndroidGradleWrapper
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.utils.addExtendsFromRelation
import java.io.File
import java.util.concurrent.Callable

class Android25ProjectHandler(
    kotlinConfigurationTools: KotlinConfigurationTools
) : AbstractAndroidProjectHandler(kotlinConfigurationTools) {

    override fun wireKotlinTasks(
        project: Project,
        compilation: KotlinJvmAndroidCompilation,
        androidPlugin: BasePlugin,
        androidExt: BaseExtension,
        variantData: BaseVariant,
        javaTask: AbstractCompile,
        kotlinTask: KotlinCompile
    ) {
        val preJavaKotlinOutputFiles = mutableListOf<File>().apply {
            add(kotlinTask.destinationDir)
            if (Kapt3GradleSubplugin.isEnabled(project)) {
                // Add Kapt3 output as well, since there's no SyncOutputTask with the new API
                val kaptClasssesDir = Kapt3GradleSubplugin.getKaptGeneratedClassesDir(project, getVariantName(variantData))
                add(kaptClasssesDir)
            }
        }
        val preJavaKotlinOutput = project.files(*preJavaKotlinOutputFiles.toTypedArray()).builtBy(kotlinTask)

        val preJavaClasspathKey = variantData.registerPreJavacGeneratedBytecode(preJavaKotlinOutput)
        kotlinTask.dependsOn(variantData.getSourceFolders(SourceKind.JAVA))

        kotlinTask.mapClasspath {
            val kotlinClasspath = variantData.getCompileClasspath(preJavaClasspathKey)
            kotlinClasspath + project.files(AndroidGradleWrapper.getRuntimeJars(androidPlugin, androidExt))
        }

        // Find the classpath entries that comes from the tested variant and register it as the friend path, lazily
        compilation.testedVariantArtifacts.set(project.files(project.provider {
            variantData.getCompileClasspathArtifacts(preJavaClasspathKey)
                .filter { it.id.componentIdentifier is TestedComponentIdentifier }
                .map { it.file }
        }))

        kotlinTask.javaOutputDir = javaTask.destinationDir

        compilation.output.classesDirs.run {
            from(project.files(kotlinTask.taskData.destinationDir).builtBy(kotlinTask))
            from(project.files(project.provider { javaTask.destinationDir }).builtBy(javaTask))
        }
    }

    override fun getSourceProviders(variantData: BaseVariant): Iterable<SourceProvider> =
        variantData.sourceSets

    override fun getAllJavaSources(variantData: BaseVariant): Iterable<File> =
        variantData.getSourceFolders(SourceKind.JAVA).map { it.dir }

    override fun getFlavorNames(variant: BaseVariant): List<String> = variant.productFlavors.map { it.name }

    override fun getBuildTypeName(variant: BaseVariant): String = variant.buildType.name

    override fun getJavaTask(variantData: BaseVariant): AbstractCompile? {
        @Suppress("DEPRECATION") // There is always a Java compile task -- the deprecation was for Jack
        return variantData::class.java.methods.firstOrNull { it.name == "getJavaCompileProvider" }
            ?.invoke(variantData)
            ?.let { taskProvider ->
                // org.gradle.api.tasks.TaskProvider is added in Gradle 4.8
                taskProvider::class.java.methods.firstOrNull { it.name == "get" }?.invoke(taskProvider) as AbstractCompile?
            } ?: variantData.javaCompile
    }

    override fun addJavaSourceDirectoryToVariantModel(variantData: BaseVariant, javaSourceDirectory: File) =
        variantData.addJavaSourceFoldersToModel(javaSourceDirectory)

    override fun getResDirectories(variantData: BaseVariant): FileCollection {
        val getAllResourcesMethod =
            variantData::class.java.methods.firstOrNull { it.name == "getAllRawAndroidResources" }
        if (getAllResourcesMethod != null) {
            val allResources = getAllResourcesMethod.invoke(variantData) as FileCollection
            return allResources
        }

        val project = variantData.mergeResources.project
        return project.files(Callable { variantData.mergeResources?.computeResourceSetList0() ?: emptyList() })
    }

    // TODO the return type is actually `AbstractArchiveTask | TaskProvider<out AbstractArchiveTask>`;
    //      change the signature once the Android Gradle plugin versions that don't support task providers are dropped
    override fun getLibraryOutputTask(variant: BaseVariant): Any? {
        val getPackageLibraryProvider = variant.javaClass.methods
            .find { it.name == "getPackageLibraryProvider" && it.parameterCount == 0 }

        return if (getPackageLibraryProvider != null) {
            @Suppress("UNCHECKED_CAST")
            getPackageLibraryProvider(variant) as TaskProvider<out AbstractArchiveTask>
        } else {
            (variant as? LibraryVariant)?.packageLibrary
        }
    }

    override fun setUpDependencyResolution(variant: BaseVariant, compilation: KotlinJvmAndroidCompilation) {
        val project = compilation.target.project

        AbstractKotlinTargetConfigurator.defineConfigurationsForCompilation(compilation)

        compilation.compileDependencyFiles = variant.compileConfiguration.apply {
            usesPlatformOf(compilation.target)
            project.addExtendsFromRelation(name, compilation.compileDependencyConfigurationName)
        }

        compilation.runtimeDependencyFiles = variant.runtimeConfiguration.apply {
            usesPlatformOf(compilation.target)
            project.addExtendsFromRelation(name, compilation.runtimeDependencyConfigurationName)
        }

        // TODO this code depends on the convention that is present in the Android plugin as there's no public API
        // We should request such API in the Android plugin
        val apiElementsConfigurationName = "${variant.name}ApiElements"
        val runtimeElementsConfigurationName = "${variant.name}RuntimeElements"

        // KT-29476, the Android *Elements configurations need Kotlin MPP dependencies:
        if (project.configurations.findByName(apiElementsConfigurationName) != null) {
            project.addExtendsFromRelation(apiElementsConfigurationName, compilation.apiConfigurationName)
        }
        if (project.configurations.findByName(runtimeElementsConfigurationName) != null) {
            project.addExtendsFromRelation(runtimeElementsConfigurationName, compilation.implementationConfigurationName)
            project.addExtendsFromRelation(runtimeElementsConfigurationName, compilation.runtimeOnlyConfigurationName)
        }

        listOf(apiElementsConfigurationName, runtimeElementsConfigurationName).forEach { outputConfigurationName ->
            project.configurations.findByName(outputConfigurationName)?.usesPlatformOf(compilation.target)
        }
    }

    private inner class KaptVariant(variantData: BaseVariant) : KaptVariantData<BaseVariant>(variantData) {
        override val name: String = getVariantName(variantData)
        override val sourceProviders: Iterable<SourceProvider> = getSourceProviders(variantData)
        override fun addJavaSourceFoldersToModel(generatedFilesDir: File) =
            addJavaSourceDirectoryToVariantModel(variantData, generatedFilesDir)

        override val annotationProcessorOptions: Map<String, String>? =
            variantData.javaCompileOptions.annotationProcessorOptions.arguments

        override fun registerGeneratedJavaSource(
            project: Project,
            kaptTask: KaptTask,
            javaTask: AbstractCompile
        ) {
            val kaptSourceOutput = project.fileTree(kaptTask.destinationDir).builtBy(kaptTask)
            kaptSourceOutput.include("**/*.java")
            variantData.registerExternalAptJavaOutput(kaptSourceOutput)
            variantData.dataBindingDependencyArtifactsIfSupported?.let { kaptTask.dependsOn(it) }
        }

        override val annotationProcessorOptionProviders: List<*>
            get() = try {
                // Public API added in Android Gradle Plugin 3.2.0-alpha15:
                val apOptions = variantData.javaCompileOptions.annotationProcessorOptions
                apOptions.javaClass.getMethod("getCompilerArgumentProviders").invoke(apOptions) as List<*>
            } catch (e: NoSuchMethodException) {
                emptyList<Any>()
            }
    }

    //TODO A public API is expected for this purpose. Once it is available, use the public API
    private fun MergeResources.computeResourceSetList0(): List<File>? {
        val computeResourceSetListMethod = MergeResources::class.java.declaredMethods
            .firstOrNull { it.name == "computeResourceSetList" && it.parameterCount == 0 } ?: return null

        val oldIsAccessible = computeResourceSetListMethod.isAccessible
        try {
            computeResourceSetListMethod.isAccessible = true

            val resourceSets = computeResourceSetListMethod.invoke(this) as? Iterable<*>

            return resourceSets
                ?.mapNotNull { resourceSet ->
                    val getSourceFiles = resourceSet?.javaClass?.methods?.find { it.name == "getSourceFiles" && it.parameterCount == 0 }
                    val files = getSourceFiles?.invoke(resourceSet)
                    @Suppress("UNCHECKED_CAST")
                    files as? Iterable<File>
                }
                ?.flatten()

        } finally {
            computeResourceSetListMethod.isAccessible = oldIsAccessible
        }
    }

    //TODO once the Android plugin reaches its 3.0.0 release, consider compiling against it (remove the reflective call)
    private val BaseVariant.dataBindingDependencyArtifactsIfSupported: FileCollection?
        get() = this::class.java.methods
            .find { it.name == "getDataBindingDependencyArtifacts" }
            ?.also { it.isAccessible = true }
            ?.invoke(this) as? FileCollection

    override fun wrapVariantDataForKapt(variantData: BaseVariant): KaptVariantData<BaseVariant> =
        KaptVariant(variantData)
}

internal fun getTestedVariantData(variantData: BaseVariant): BaseVariant? = when (variantData) {
    is TestVariant -> variantData.testedVariant
    is UnitTestVariant -> variantData.testedVariant as? BaseVariant
    else -> null
}

internal fun getVariantName(variant: BaseVariant): String = variant.name
