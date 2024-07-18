/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.component.LibraryBinaryIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.SwiftExportTaskParameters
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.SwiftExportAction
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.SwiftExportedModule
import org.jetbrains.kotlin.gradle.utils.LazyResolvedConfiguration
import org.jetbrains.kotlin.gradle.utils.dashSeparatedToUpperCamelCase
import org.jetbrains.kotlin.gradle.utils.getFile
import java.io.File
import javax.inject.Inject

@DisableCachingByDefault(because = "Swift Export is experimental, so no caching for now")
internal abstract class SwiftExportTask @Inject constructor(
    private val workerExecutor: WorkerExecutor,
    private val fileSystem: FileSystemOperations,
) : DefaultTask() {

    @get:Internal
    abstract val configuration: Property<LazyResolvedConfiguration>

    @get:Input
    abstract val mainModuleName: Property<String>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val mainArtifact: RegularFileProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val swiftExportClasspath: ConfigurableFileCollection

    @get:Nested
    abstract val parameters: SwiftExportTaskParameters

    @TaskAction
    fun run() {
        cleanup()

        val swiftExportQueue = workerExecutor.classLoaderIsolation { workerSpec ->
            workerSpec.classpath.from(swiftExportClasspath)
        }

        swiftExportQueue.submit(SwiftExportAction::class.java) { workParameters ->
            workParameters.bridgeModuleName.set(parameters.bridgeModuleName)
            workParameters.konanDistribution.set(parameters.konanDistribution)
            workParameters.outputPath.set(parameters.outputPath)
            workParameters.stableDeclarationsOrder.set(parameters.stableDeclarationsOrder)
            workParameters.swiftModules.set(swiftExportedModules())
            workParameters.swiftModulesFile.set(parameters.swiftModulesFile)
        }
    }

    private fun cleanup() {
        fileSystem.delete {
            it.delete(parameters.outputPath)
        }
    }

    private fun swiftExportedModules(): Provider<List<SwiftExportedModule>> {
        return configuration.map { configuration ->
            configuration.swiftExportedModules()
        }.map { modules ->
            modules.toMutableList().apply {
                add(SwiftExportedModule(mainModuleName.get(), mainArtifact.getFile()))
            }
        }
    }
}

private val File.isCinteropKlib get() = extension == "klib" && nameWithoutExtension.contains("cinterop-interop")

internal fun Collection<File>.filterNotCinteropKlibs(): List<File> = filterNot(File::isCinteropKlib)

internal fun LazyResolvedConfiguration.swiftExportedModules(): List<SwiftExportedModule> {
    return allResolvedDependencies.asSequence().filterNot { dependencyResult ->
        dependencyResult.resolvedVariant.owner.let { id -> id is ModuleComponentIdentifier && id.module == "kotlin-stdlib" }
    }.map { it.selected }.map { component ->
        val dependencyArtifacts = getArtifacts(component)
            .map { it.file }
            .filterNotCinteropKlibs()

        if (dependencyArtifacts.isEmpty() || dependencyArtifacts.size > 1) {
            throw AssertionError(
                "Component $component ${
                    if (dependencyArtifacts.isEmpty())
                        "doesn't have suitable artifacts"
                    else
                        "has too many artifacts: $dependencyArtifacts"
                }"
            )
        }

        Pair(component, dependencyArtifacts.single())
    }.distinctBy { (_, artifact) ->
        artifact
    }.map { (component, artifact) ->
        when (val dependencyModule = component.id) {
            is ProjectComponentIdentifier -> dashSeparatedToUpperCamelCase(dependencyModule.projectName)
            is ModuleComponentIdentifier -> dashSeparatedToUpperCamelCase(dependencyModule.moduleIdentifier.name)
            is LibraryBinaryIdentifier -> dashSeparatedToUpperCamelCase(dependencyModule.libraryName)
            else -> throw AssertionError("Unsupported component $component")
        }.let { SwiftExportedModule(it, artifact) }
    }.toList()
}