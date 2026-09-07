/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnostic
import org.jetbrains.kotlin.gradle.plugin.diagnostics.UsesKotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.SwiftExportAction
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.SwiftExportTaskParameters
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.SwiftExportedDependency
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.SwiftExportedModule
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.collectModules
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.createFullyExportedSwiftExportedModule
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.createTransitiveSwiftExportedModule
import org.jetbrains.kotlin.gradle.plugin.mpp.export.internal.SwiftExportDeclaredModuleOptions
import org.jetbrains.kotlin.gradle.plugin.mpp.export.internal.SwiftExportDependencySelector
import org.jetbrains.kotlin.gradle.targets.native.toolchain.KotlinNativeProvider
import org.jetbrains.kotlin.gradle.utils.LazyResolvedConfigurationWithArtifacts
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.konan.target.Distribution
import javax.inject.Inject

@DisableCachingByDefault(because = "Swift Export is experimental, so no caching for now")
internal abstract class SwiftExportTask @Inject constructor(
    private val workerExecutor: WorkerExecutor,
    private val fileSystem: FileSystemOperations,
) : DefaultTask(), UsesKotlinToolingDiagnostics {

    internal abstract class ModuleInput {
        @get:Input
        abstract val moduleName: Property<String>

        @get:Input
        @get:Optional
        abstract val flattenPackage: Property<String>

        @get:InputFiles
        @get:PathSensitive(PathSensitivity.RELATIVE)
        abstract val artifact: RegularFileProperty
    }

    @get:Nested
    abstract val mainModuleInput: ModuleInput

    @get:Input
    @get:Optional
    abstract val cinteropModuleName: Property<String>

    @get:InputFiles
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val cinteropModuleArtifact: RegularFileProperty

    @get:Nested
    abstract val kotlinNativeProvider: Property<KotlinNativeProvider>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val swiftExportClasspath: ConfigurableFileCollection

    @get:Nested
    abstract val parameters: SwiftExportTaskParameters

    /**
     * The dependency graph the exported modules are resolved from. Held as Configuration-Cache-safe
     * [LazyResolvedConfigurationWithArtifacts] rather than a precomputed module list so that the resolution happens at
     * execution time (see [run]); up-to-date checking is provided by the raw configurations wired as task inputs in
     * `registerSwiftExportRun`.
     */
    @get:Internal
    abstract val exportConfiguration: Property<LazyResolvedConfigurationWithArtifacts>

    @get:Internal
    abstract val apiConfiguration: Property<LazyResolvedConfigurationWithArtifacts>

    @get:Internal
    abstract val metadataConfiguration: Property<LazyResolvedConfigurationWithArtifacts>

    @get:Internal
    abstract val exportedModules: SetProperty<SwiftExportedDependency>

    @get:Internal
    abstract val dependencyOptionsOverrides: MapProperty<SwiftExportDependencySelector, SwiftExportDeclaredModuleOptions>

    @get:Input
    internal val overridesInputs: List<String>
        get() = dependencyOptionsOverrides.get()
            .map { (selector, options) ->
                val selectorKey = when (selector) {
                    is SwiftExportDependencySelector.ProjectPath -> "project|${selector.projectPath}"
                    is SwiftExportDependencySelector.Module -> "module|${selector.group}|${selector.name}"
                }
                "$selectorKey|${options.moduleName}|${options.rootPackage}"
            }
            .sorted()

    @get:Internal
    abstract val ignoreExperimentalDiagnostic: Property<Boolean>

    @TaskAction
    fun run() {
        if (!ignoreExperimentalDiagnostic.get()) {
            warnAboutExperimentalSwiftExportFeature()
        }

        cleanup()

        // Run Swift Export with process isolation to avoid leakage for AA/IntelliJ classes. See KT-73438
        val swiftExportQueue = workerExecutor.processIsolation { workerSpec ->
            workerSpec.classpath.from(swiftExportClasspath)
            // With this flag of true, we would have to embed kotlinx.coroutines.internal.intellij.IntellijCoroutines into somewhere,
            // to avoid ClassNotFoundException. As it's currently unclear how to embed this class, we disable it for now.
            workerSpec.forkOptions.systemProperties.put("ide.can.use.coroutines.fork", "false")
        }

        // The exported-module list is resolved here, at execution time, rather than during configuration: the resolved
        // configurations only need to be readable when the task runs, which keeps the resolution off the
        // configuration-cache store phase.
        val swiftModules = buildList {
            addAll(resolveSwiftExportedModules())
            add(
                createFullyExportedSwiftExportedModule(
                    mainModuleInput.moduleName.get(),
                    mainModuleInput.flattenPackage.orNull,
                    mainModuleInput.artifact.getFile()
                )
            )
            if (cinteropModuleName.isPresent) {
                add(
                    createTransitiveSwiftExportedModule(
                        moduleName = cinteropModuleName.get(),
                        artifact = cinteropModuleArtifact.getFile()
                    )
                )
            }
        }

        swiftExportQueue.submit(SwiftExportAction::class.java) { workParameters ->
            workParameters.bridgeModuleName.set(parameters.bridgeModuleName)
            workParameters.outputPath.set(parameters.outputPath)
            workParameters.stableDeclarationsOrder.set(parameters.stableDeclarationsOrder)
            workParameters.swiftModulesFile.set(parameters.swiftModulesFile)
            workParameters.swiftModules.set(swiftModules)
            workParameters.swiftExportSettings.set(parameters.swiftExportSettings)
            workParameters.konanDistribution.set(kotlinNativeProvider.flatMap { it.bundleDirectory }.map { Distribution(it) })
            workParameters.konanTarget.set(parameters.konanTarget)
        }
    }

    /**
     * Resolves the transitive and explicitly-exported Swift modules from the dependency graph. This is done at
     * execution time (called from [run]), reading the Configuration-Cache-safe holders resolved during configuration.
     * The main module and the optional cinterop module are appended by [run]; they are not part of this result.
     *
     * Exposed as `internal` so functional tests can assert the resolved module set without executing the whole task.
     * [reportDiagnostic] defaults to this task's execution-time reporter; tests may pass a project-scoped reporter to
     * observe resolution diagnostics through the regular collector.
     */
    internal fun resolveSwiftExportedModules(
        reportDiagnostic: (ToolingDiagnostic) -> Unit = { this.reportDiagnostic(it) },
    ): List<SwiftExportedModule> = collectModules(
        exportConfiguration = exportConfiguration.get(),
        apiConfiguration = apiConfiguration.orNull,
        metadataConfiguration = metadataConfiguration.orNull,
        exportedModules = exportedModules.get(),
        dependencyOptionsOverrides = dependencyOptionsOverrides.get(),
        rootModuleName = mainModuleInput.moduleName.get(),
        reportDiagnostic = reportDiagnostic,
    )

    private fun cleanup() {
        fileSystem.delete {
            it.delete(parameters.outputPath)
        }
    }

    private fun warnAboutExperimentalSwiftExportFeature() {
        reportDiagnostic(
            KotlinToolingDiagnostics.ExperimentalFeatureWarning(
                "Swift Export",
                "https://kotl.in/1cr522",
                "To suppress this message add '${PropertiesProvider.PropertyNames.KOTLIN_SWIFT_EXPORT_EXPERIMENTAL_NOWARN}=true' to your gradle.properties"
            )
        )
    }
}
