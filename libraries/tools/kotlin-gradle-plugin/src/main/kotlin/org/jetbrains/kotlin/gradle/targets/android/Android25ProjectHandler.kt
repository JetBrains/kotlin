/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin

import com.android.build.api.attributes.BuildTypeAttr
import com.android.build.gradle.*
import com.android.build.gradle.api.*
import com.android.build.gradle.tasks.MergeResources
import org.gradle.api.Project
import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.FileCollection
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.internal.Kapt3GradleSubplugin
import org.jetbrains.kotlin.gradle.plugin.android.AndroidGradleWrapper
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.thisTaskProvider
import org.jetbrains.kotlin.gradle.utils.addExtendsFromRelation
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.util.concurrent.Callable

class Android25ProjectHandler(
    kotlinConfigurationTools: KotlinConfigurationTools
) : AbstractAndroidProjectHandler(kotlinConfigurationTools) {

    override fun wireKotlinTasks(
        project: Project,
        compilation: KotlinJvmAndroidCompilation,
        androidPlugin: BasePlugin<*>,
        androidExt: BaseExtension,
        variantData: BaseVariant,
        javaTask: TaskProvider<out AbstractCompile>,
        kotlinTask: TaskProvider<out KotlinCompile>
    ) {
        val preJavaKotlinOutput = project.files(project.provider {
            mutableListOf<File>().apply {
                add(kotlinTask.get().destinationDir)
                if (Kapt3GradleSubplugin.isEnabled(project)) {
                    // Add Kapt3 output as well, since there's no SyncOutputTask with the new API
                    val kaptClasssesDir = Kapt3GradleSubplugin.getKaptGeneratedClassesDir(project, getVariantName(variantData))
                    add(kaptClasssesDir)
                }
            }
        }).builtBy(kotlinTask)

        val preJavaClasspathKey = variantData.registerPreJavacGeneratedBytecode(preJavaKotlinOutput)
        kotlinTask.configure { kotlinTaskInstance ->
            kotlinTaskInstance.inputs.files(variantData.getSourceFolders(SourceKind.JAVA)).withPathSensitivity(PathSensitivity.RELATIVE)

            kotlinTaskInstance.classpath = project.files()
                .from(variantData.getCompileClasspath(preJavaClasspathKey))
                .from(Callable { AndroidGradleWrapper.getRuntimeJars(androidPlugin, androidExt) })

            kotlinTaskInstance.javaOutputDir.set(javaTask.flatMap { it.destinationDirectory })
        }

        // Find the classpath entries that come from the tested variant and register them as the friend paths, lazily
        val originalArtifactCollection = variantData.getCompileClasspathArtifacts(preJavaClasspathKey)
        val testedVariantDataIsNotNull = getTestedVariantData(variantData) != null
        val projectPath = project.path
        compilation.testedVariantArtifacts.set(
            originalArtifactCollection.artifactFiles.filter(
                AndroidTestedVariantArtifactsFilter(
                    originalArtifactCollection,
                    testedVariantDataIsNotNull,
                    projectPath
                )
            )
        )

        compilation.output.classesDirs.from(
            kotlinTask.flatMap { it.destinationDirectory },
            javaTask.flatMap { it.destinationDirectory }
        )
    }

    override fun getFlavorNames(variant: BaseVariant): List<String> = variant.productFlavors.map { it.name }

    override fun getBuildTypeName(variant: BaseVariant): String = variant.buildType.name

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

        val buildTypeAttrValue = project.objects.named(BuildTypeAttr::class.java, variant.buildType.name)
        listOf(compilation.compileDependencyConfigurationName, compilation.runtimeDependencyConfigurationName).forEach {
            project.configurations.findByName(it)?.attributes?.attribute(Attribute.of(BuildTypeAttr::class.java), buildTypeAttrValue)
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
}

internal fun getTestedVariantData(variantData: BaseVariant): BaseVariant? = when (variantData) {
    is TestVariant -> variantData.testedVariant
    is UnitTestVariant -> variantData.testedVariant as? BaseVariant
    else -> null
}

internal fun getVariantName(variant: BaseVariant): String = variant.name

@Suppress("UNCHECKED_CAST")
internal fun BaseVariant.getJavaTaskProvider(): TaskProvider<out JavaCompile> =
    this::class.java.methods.firstOrNull { it.name == "getJavaCompileProvider" }
        ?.invoke(this) as? TaskProvider<JavaCompile>
        ?: @Suppress("DEPRECATION") javaCompile.thisTaskProvider

internal fun forEachVariant(project: Project, action: (BaseVariant) -> Unit) {
    val androidExtension = project.extensions.getByName("android")
    when (androidExtension) {
        is AppExtension -> androidExtension.applicationVariants.all(action)
        is LibraryExtension -> {
            androidExtension.libraryVariants.all(action)
            if (androidExtension is FeatureExtension) {
                androidExtension.featureVariants.all(action)
            }
        }
        is TestExtension -> androidExtension.applicationVariants.all(action)
    }
    if (androidExtension is TestedExtension) {
        androidExtension.testVariants.all(action)
        androidExtension.unitTestVariants.all(action)
    }
}

internal fun BaseVariant.getResDirectories(): FileCollection {
    val getAllResourcesMethod =
        this::class.java.methods.firstOrNull { it.name == "getAllRawAndroidResources" }
    if (getAllResourcesMethod != null) {
        val allResources = getAllResourcesMethod.invoke(this) as FileCollection
        return allResources
    }

    val project = mergeResources.project
    return project.files(Callable { mergeResources?.computeResourceSetList0() ?: emptyList() })
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

/** Filter for the AGP test variant classpath artifacts. */
class AndroidTestedVariantArtifactsFilter(
    private val artifactCollection: ArtifactCollection,
    private val testedVariantDataIsNotNull: Boolean,
    private val projectPath: String
) : Serializable, Spec<File> {

    /** Make transient as it should be derived from the [artifactCollection] property which may change in configuration cached runs. */
    @Transient
    private var filteredFiles = initFilteredFiles()

    private fun initFilteredFiles(): Lazy<Set<File>> {
        return lazy {
            artifactCollection.filter {
                it.id.componentIdentifier is TestedComponentIdentifier ||
                        // If tests depend on the main classes transitively, through a test dependency on another module which
                        // depends on this module, then there's no artifact with a TestedComponentIdentifier, so consider the artifact of the
                        // current module a friend path, too:
                        testedVariantDataIsNotNull &&
                        (it.id.componentIdentifier as? ProjectComponentIdentifier)?.projectPath == projectPath
            }
                .mapTo(mutableSetOf()) { it.file }
        }
    }

    private fun writeObject(objectOutputStream: ObjectOutputStream) {
        objectOutputStream.defaultWriteObject()
    }

    private fun readObject(objectInputStream: ObjectInputStream) {
        objectInputStream.defaultReadObject()
        filteredFiles = initFilteredFiles()
    }

    override fun isSatisfiedBy(element: File): Boolean {
        return element in filteredFiles.value
    }
}