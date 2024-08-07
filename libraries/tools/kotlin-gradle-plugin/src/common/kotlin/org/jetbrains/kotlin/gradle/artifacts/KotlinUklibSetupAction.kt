/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.artifacts

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.HasAttributes
import org.gradle.api.attributes.Usage
import org.gradle.api.file.*
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.gradle.dsl.awaitMetadataTarget
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.MAIN_COMPILATION_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage.AfterFinaliseCompilations
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.launch
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.tasks.dependsOn
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.maybeCreateResolvable
import org.jetbrains.kotlin.gradle.utils.named
import org.jetbrains.kotlin.gradle.utils.setAttribute
import org.jetbrains.kotlin.tooling.core.withClosure
import java.io.File
import javax.inject.Inject

private val artifactType: Attribute<String> get() = Attribute.of("artifactType", String::class.java)
private val uklibArtifactType: String get() = "uklib"
private val uklibUnzippedArtifactType: String get() = "uklib-unzipped"

internal val KotlinUklibSetupAction = KotlinProjectSetupAction {
    project.dependencies.registerTransform(
        UnzipTransform::class.java
    ) {
        it.from.attribute(artifactType, uklibArtifactType)
        it.to.attribute(artifactType, uklibUnzippedArtifactType)
    }

    project.launch {
        setupPublication()
    }

    project.launch {
        setupConsumption()
    }
}

private suspend fun Project.setupConsumption() {
    val sourceSets = multiplatformExtension.awaitSourceSets()
    val metadataTarget = multiplatformExtension.awaitMetadataTarget()
    val targets = multiplatformExtension.awaitTargets()
    AfterFinaliseCompilations.await()

    // Это точно ок работает с тестовыми сорсетами?
    val sourceSetToTargets = mutableMapOf<KotlinSourceSet, MutableSet<String>>()
    targets.flatMap { it.compilations.toList() }.forEach { compilation ->
        compilation.defaultSourceSet.withClosure { it.dependsOn }.forEach { sourceSet ->
            sourceSetToTargets.getOrPut(
                sourceSet, { mutableSetOf() }
            ).add(compilation.target.targetName)
        }
    }

    val thisModuleUklib = uklibFromKGPModel(targets.toList())
    val dependenciesAsUklibs = lazy {
        // FIXME: Do configurations have stable order in terms of dependencies and in terms of files that you put into them?
        uklibsPath.incoming.artifactView { it.isLenient = true }.artifacts.artifactFiles.map { unzippedKlib ->
            Uklib.deserializeFromDirectory(unzippedKlib)
        }
    }
    val resolvedFragments = lazy {
        resolveModuleFragmentClasspath(
            module = thisModuleUklib,
            dependencies = dependenciesAsUklibs.value.toHashSet(),
        )
    }

    // Резолвить фрагменты зависимостей нужно только для метадатных компиляций
    multiplatformExtension.targets.all { target ->
        if (target is KotlinMetadataTarget) {
            setupMetadataCompilationsUklibConsumption(target, sourceSetToTargets, lazy { resolvedFragments.value.fullFragmentClasspath })
        } else {
            val compilation = target.compilations.getByName(MAIN_COMPILATION_NAME)
            uklibsPath.extendsFrom(
                compilation.internal.configurations.apiConfiguration,
                compilation.internal.configurations.implementationConfiguration,
            )
            val fragmentFileForPlatformCompilation = provider {
                compilation.fragmentFiles(
                    sourceSetToTargets = sourceSetToTargets,
                    resolvedFragments = resolvedFragments.value.exactlyMatchingFragmentClasspath,
                )
            }
            project.tasks.register("_resolveUklibsFor${compilation.defaultSourceSet.name}") {
                it.doLast {
                    fragmentFileForPlatformCompilation.get().forEach {
                        println(it)
                    }
                }
            }
            compilation.compileDependencyFiles += files(fragmentFileForPlatformCompilation)
        }
    }
}

private fun KotlinCompilation<*>.fragmentFiles(
    sourceSetToTargets: MutableMap<KotlinSourceSet, MutableSet<String>>,
    resolvedFragments: Map<Fragment<String>, List<File>>,
): List<File> = resolvedFragments[
    Fragment(
        defaultSourceSet.name,
        sourceSetToTargets[defaultSourceSet]!!,
    )
]!!

private fun Project.setupMetadataCompilationsUklibConsumption(
    target: KotlinTarget,
    sourceSetToTargets: MutableMap<KotlinSourceSet, MutableSet<String>>,
    resolvedFragments: Lazy<Map<Fragment<String>, List<File>>>,
) {
    target.compilations.all { compilation ->
        uklibsPath.extendsFrom(
            compilation.internal.configurations.apiConfiguration,
            compilation.internal.configurations.implementationConfiguration,
        )

        if (compilation is KotlinCommonCompilation && !compilation.isKlibCompilation) return@all

        val fragmentFilesForMetadataCompilation = provider {
            compilation.fragmentFiles(
                sourceSetToTargets = sourceSetToTargets,
                resolvedFragments = resolvedFragments.value,
            )
        }
        project.tasks.register("_resolveUklibsFor${compilation.defaultSourceSet.name}") {
            it.doLast {
                fragmentFilesForMetadataCompilation.get().forEach {
                    println(it)
                }
            }
        }
        compilation.compileDependencyFiles += files(fragmentFilesForMetadataCompilation)
    }
}


private suspend fun Project.setupPublication() {
    val sourceSets = multiplatformExtension.awaitSourceSets()
    val metadataTarget = multiplatformExtension.awaitMetadataTarget()
    val targets = multiplatformExtension.awaitTargets()
    AfterFinaliseCompilations.await()

    val packUklib = tasks.register("packUklib", UklibArchiveTask::class.java)

    val kgpModel = uklibFromKGPModel(
        targets = targets.toList(),
        onPublishCompilation = {
            packUklib.dependsOn(it.compileTaskProvider)
        }
    )

    packUklib.configure {
        it.model.set(kgpModel)
    }
    artifacts.add(metadataTarget.uklibElementsConfigurationName, packUklib)
}

private fun uklibFromKGPModel(
    targets: List<KotlinTarget>,
    onPublishCompilation: (KotlinCompilation<*>) -> Unit = {}
): Uklib<String> {
    val compilationToArtifact = mutableMapOf<KotlinCompilation<*>, Iterable<File>>()

    targets.forEach { target ->
        when (target) {
            is KotlinJsIrTarget -> {
                error("...")
//                    val mainComp = target.compilations.getByName(MAIN_COMPILATION_NAME)
//                    compilationToArtifact[mainComp] = mainComp.out
            }
            is KotlinJvmTarget -> {
                val mainComp = target.compilations.getByName(MAIN_COMPILATION_NAME)
                // FIXME: How do we handle that there are multiple classesDir?
                compilationToArtifact[mainComp] = mainComp.output.classesDirs
                onPublishCompilation(mainComp)
            }
            is KotlinNativeTarget -> {
                val mainComp = target.compilations.getByName(MAIN_COMPILATION_NAME)
                compilationToArtifact[mainComp] = listOf(
                    // FIXME: Make this lazy
                    // FIXME: We have to unzip this
                    mainComp.compileTaskProvider.flatMap {
                        it.outputFile
                    }.get()
                )
                onPublishCompilation(mainComp)
            }
            // FIXME: Metadata target forms a natural bamboo with default target hierarchy
            is KotlinMetadataTarget -> {
                target.compilations
                    // Probably this is not needed
                    .filterNot { it is KotlinCommonCompilation && !it.isKlibCompilation }
                    .forEach { compilation ->
                        // FIXME: Aren't test compilations going to be here?
                        compilationToArtifact[compilation] = compilation.output.classesDirs
                        onPublishCompilation(compilation)
                    }
            }
        }
    }

    return transformKGPModelToUklibModel(
        "stub",
        publishedCompilations = compilationToArtifact.keys.toList(),
        publishedArtifact = { compilationToArtifact[this]!!.single() },
        defaultSourceSet = { this.defaultSourceSet },
        target = { this.target.targetName },
        dependsOn = { this.dependsOn },
        identifier = { this.name }
    )
}

internal abstract class UklibArchiveTask : DefaultTask() {
    @get:Internal
    abstract val model: Property<Uklib<String>>

    @get:OutputFile
    val outputZip: RegularFileProperty = project.objects.fileProperty().convention(
        project.layout.buildDirectory.file("output.uklib")
    )

    @get:Internal
    val temporariesDirectory: DirectoryProperty = project.objects.directoryProperty().convention(
        project.layout.buildDirectory.dir("uklibTemp")
    )

    @TaskAction
    fun run() {
        model.get().serializeUklibToArchive(
            outputZip = outputZip.getFile(),
            temporariesDirectory = temporariesDirectory.getFile(),
        )
    }
}

abstract class UnzipTransform @Inject constructor(
    private val fileOperations: FileSystemOperations,
    private val archiveOperations: ArchiveOperations,
) : TransformAction<TransformParameters.None> {
    @get:InputArtifact
    abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        fileOperations.copy {
            it.from(archiveOperations.zipTree(inputArtifact.get().asFile))
            it.into(outputs.dir("unzipped"))
        }
    }
}

//internal abstract class ConsumeUKlibDependenciesTask : DefaultTask() {
//    @get:InputFiles
//    val uklibs: ConfigurableFileCollection = project.files()
//
//    @get:OutputFiles
//    val perTargetOutputs: DirectoryProperty = project.objects.directoryProperty().convention(
//        project.layout.buildDirectory.dir("uklibPerTargetOutputs")
//    )
//}

internal fun HasAttributes.configureUklibConfigurationAttributes(project: Project) {
    attributes.setAttribute(Category.CATEGORY_ATTRIBUTE, project.objects.named(Category.LIBRARY))
    attributes.setAttribute(Usage.USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_UKLIB))
}

internal val Project.uklibsPath: Configuration get() = configurations.maybeCreateResolvable("uklibPath") {
    configureUklibConfigurationAttributes(project)
    attributes.setAttribute(artifactType, uklibUnzippedArtifactType)
}