/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.commonizer.api.*
import org.jetbrains.kotlin.compilerRunner.konanHome
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.KLIB_COMMONIZER_CLASSPATH_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinSharedNativeCompilation
import org.jetbrains.kotlin.gradle.targets.native.internal.CInteropCommonizerTask.CInteropLibraryProvider
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import java.io.File

open class CInteropCommonizerTask : DefaultTask() {

    @get:Nested
    internal var cinterops = setOf<CInteropLibraryProvider>()
        private set


    fun from(vararg tasks: CInteropProcess) {
        cinterops += tasks.toList()
            .onEach { task -> this.dependsOn(task) }
            .map { task ->
                CInteropLibraryProvider(
                    task.settings.name,
                    task.settings.compilation,
                    task.outputFileProvider,
                    task.settings.dependencyFiles // TODO NOW: var shall be replaced by provider
                )
            }
    }

    internal fun from(vararg cinterop: CInteropLibraryProvider) {
        from(cinterop.toList())
    }

    fun from(cinterops: List<CInteropLibraryProvider>) {
        this.cinterops += cinterops
    }


    data class CInteropLibraryProvider(
        @get:Input val name: String,
        @get:Internal val compilation: KotlinNativeCompilation,
        @get:Classpath val libraryFile: Provider<File>,
        @get:Classpath val dependencies: FileCollection
    ) {
        @Suppress("unused") // Used for UP-TO-DATE check
        @get:Input
        val sourceSetNames
            get() = compilation.allKotlinSourceSets.map { it.name }.toSet()

        @get:Input
        val konanTarget
            get() = compilation.konanTarget
    }

    @TaskAction
    internal fun commonizeCInteropLibraries() {
        val commonizerRequestsForSharedCompilations = project.commonizerRequestsForSharedCompilations(cinterops)
        for (interopGists in commonizerRequestsForSharedCompilations.map { it.cinterops }.toSet()) {
            commonize(interopGists)
        }
    }

    private fun commonize(cinterops: Set<CInteropLibraryProvider>) {
        outputDirectory(cinterops).deleteRecursively()
        val commonizer = CliCommonizer(project.configurations.getByName(KLIB_COMMONIZER_CLASSPATH_CONFIGURATION_NAME).resolve())
        commonizer(
            konanHome = project.file(project.konanHome),
            targetLibraries = cinterops.map { it.libraryFile.get() }.toSet(),
            dependencyLibraries = cinterops.flatMap { it.dependencies.files }.toSet(),
            outputHierarchy = SharedCommonizerTarget(cinterops.map { CommonizerTarget(it.konanTarget) }.toSet()),
            outputDirectory = outputDirectory(cinterops)
        )
    }

    private fun outputDirectory(cinterops: Set<CInteropLibraryProvider>): File {
        return project.rootDir.resolve(".gradle/kotlin/commonizer/cinterop")
            .resolve(project.path)
            .resolve(cinterops.joinToString("-") { it.name })
    }

    private fun commonizedOutputDirectory(cinterops: Set<CInteropLibraryProvider>): File {
        return HierarchicalCommonizerOutputLayout.getTargetDirectory(
            outputDirectory(cinterops), SharedCommonizerTarget(cinterops.map { it.konanTarget }.map { LeafCommonizerTarget(it) }.toSet())
        )
    }

    internal fun commonizedOutputDirectory(compilation: KotlinSharedNativeCompilation): FileCollection {
        val request = commonizerRequestForSharedCompilation(compilation, cinterops) ?: return project.files()
        return project.files(project.provider {
            commonizedOutputDirectory(request.cinterops).listFiles().orEmpty()
        }).builtBy(this)
    }
}

private data class SharedNativeCompilationCommonizerRequest(
    val sharedNativeCompilation: KotlinSharedNativeCompilation, val cinterops: Set<CInteropLibraryProvider>
)

private fun Project.commonizerRequestsForSharedCompilations(
    cinterops: Set<CInteropLibraryProvider>
): Set<SharedNativeCompilationCommonizerRequest> {
    val multiplatformExtension = project.multiplatformExtensionOrNull ?: return emptySet()
    return multiplatformExtension.targets.flatMap { it.compilations }
        .filterIsInstance<KotlinSharedNativeCompilation>()
        .mapNotNull { sharedCompilation -> commonizerRequestForSharedCompilation(sharedCompilation, cinterops) }
        .toSet()
}

private fun commonizerRequestForSharedCompilation(
    compilation: KotlinSharedNativeCompilation,
    cinterops: Set<CInteropLibraryProvider>
): SharedNativeCompilationCommonizerRequest? {
    // TODO NOW: This is not correct
    val cinteropsForCompilation = cinterops
        .filter { cinterop -> cinterop.sourceSetNames.containsAll(compilation.allKotlinSourceSets.map { it.name }) }
        .toSet()

    if (cinteropsForCompilation.isEmpty()) return null
    return SharedNativeCompilationCommonizerRequest(compilation, cinteropsForCompilation)
}
