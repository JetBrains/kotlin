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
import org.jetbrains.kotlin.Uklib
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
import org.jetbrains.kotlin.resolveFragmentRefinersWithinModule
import org.jetbrains.kotlin.resolveModuleFragmentClasspath
import org.jetbrains.kotlin.tooling.core.withClosure
import org.jetbrains.kotlin.transformKGPModelToUklibModel
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

//        val kgpModel = transformKGPModelToUklibModel(
//            "foo",
//            publishedCompilations = compilationToArtifact.keys.toList(),
//            publishedArtifact = { compilationToArtifact[this]!!.single() },
//            defaultSourceSet = { this.defaultSourceSet },
//            target = { this.target.targetName },
//            dependsOn = { this.dependsOn },
//            identifier = { this.name }
//        )
//
//        resolveModuleFragmentClasspath
        val dependenciesAsUklibs = lazy {
            uklibsPath.map { unzippedKlib ->
                Uklib.deserializeFromDirectory(unzippedKlib)
            }
        }

        multiplatformExtension.targets.configureEach { target ->
            target.compilations.configureEach { compilation ->
                compilation.internal.configurations.compileDependencyConfiguration.dependencies.addLater(
                    provider {
                        project.dependencies.create(
                            project.files(
                                // Uklibs path are
                                uklibsPath.map { unzippedKlib ->
                                    Uklib.deserializeFromDirectory(unzippedKlib)
                                }
                            )
                        )
                    }
                )
            }
        }
    }
}

private fun resolveUklibClasspath(
    unzippedUklib: File,
    targets: Set<String>,
): List<File> {
    resolveModuleFragmentClasspath(

    )
    Uklib.deserializeFromDirectory(unzippedUklib)
}

private suspend fun Project.setupPublication() {
    val sourceSets = multiplatformExtension.awaitSourceSets()
    val metadataTarget = multiplatformExtension.awaitMetadataTarget()
    val targets = multiplatformExtension.awaitTargets()
    AfterFinaliseCompilations.await()

    // FIXME: How do we handle that there are multiple classesDir?
    val compilationToArtifact = mutableMapOf<KotlinCompilation<*>, Iterable<File>>()
    val packUklib = tasks.register("packUklib", UklibArchiveTask::class.java)

    targets.forEach { target ->
        when (target) {
            is KotlinJsIrTarget -> {
                error("...")
//                    val mainComp = target.compilations.getByName(MAIN_COMPILATION_NAME)
//                    compilationToArtifact[mainComp] = mainComp.out
            }
            is KotlinJvmTarget -> {
                val mainComp = target.compilations.getByName(MAIN_COMPILATION_NAME)
                compilationToArtifact[mainComp] = mainComp.output.classesDirs
                packUklib.dependsOn(mainComp.compileTaskProvider)
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
                packUklib.dependsOn(mainComp.compileTaskProvider)
            }
            // FIXME: Metadata target forms a natural bamboo with default target hierarchy
            is KotlinMetadataTarget -> {
                target.compilations
                    // Probably this is not needed
                    .filterNot { it is KotlinCommonCompilation && !it.isKlibCompilation }
                    .forEach { compilation ->
                        // FIXME: Aren't test compilations going to be here?
                        compilationToArtifact[compilation] = compilation.output.classesDirs
                        packUklib.dependsOn(compilation.compileTaskProvider)
                    }
            }
        }
    }

    val uklibElements = configurations.getByName(metadataTarget.uklibElementsConfigurationName)

    val kgpModel = transformKGPModelToUklibModel(
        "foo",
        publishedCompilations = compilationToArtifact.keys.toList(),
        publishedArtifact = { compilationToArtifact[this]!!.single() },
        defaultSourceSet = { this.defaultSourceSet },
        target = { this.target.targetName },
        dependsOn = { this.dependsOn },
        identifier = { this.name }
    )

    packUklib.configure {
        it.model.set(kgpModel)
    }
    artifacts.add(uklibElements.name, packUklib)
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