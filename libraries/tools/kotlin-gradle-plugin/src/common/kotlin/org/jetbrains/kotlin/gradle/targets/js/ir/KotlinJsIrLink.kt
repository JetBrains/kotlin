/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.NormalizeLineEndings
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer.ContributeCompilerArgumentsContext
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.statistics.CompileKotlinJsIrLinkMetrics
import org.jetbrains.kotlin.gradle.plugin.statistics.CompileKotlinWasmIrLinkMetrics
import org.jetbrains.kotlin.gradle.plugin.statistics.UsesBuildFusService
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode.DEVELOPMENT
import org.jetbrains.kotlin.gradle.tasks.K2MultiplatformStructure
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.utils.KotlinJsCompilerOptionsDefault
import javax.inject.Inject

@CacheableTask
abstract class KotlinJsIrLink @Inject constructor(
    project: Project,
    target: KotlinPlatformType,
    objectFactory: ObjectFactory,
    workerExecutor: WorkerExecutor,
) : Kotlin2JsCompile(
    objectFactory.KotlinJsCompilerOptionsDefault(project),
    objectFactory,
    workerExecutor
), UsesBuildFusService {

    init {
        // Not check sources, only klib module
        disallowSourceChanges()
    }

    @get:Internal
    override val sources: FileCollection = super.sources

    /**
     * [K2MultiplatformStructure] is not required for JS IR link
     */
    @InternalKotlinGradlePluginApi
    @get:Internal
    override val multiplatformStructure: K2MultiplatformStructure get() = super.multiplatformStructure

    override fun skipCondition(): Boolean {
        return !entryModule.get().asFile.exists()
    }

    @Transient
    @get:Internal
    internal val propertiesProvider = PropertiesProvider(project)

    @get:Input
    var incrementalJsIr: Boolean = propertiesProvider.incrementalJsIr

    @get:Input
    var incrementalWasm: Boolean = propertiesProvider.incrementalWasm

    @get:Input
    val outputGranularity: KotlinJsIrOutputGranularity = propertiesProvider.jsIrOutputGranularity

    // Incremental stuff of link task is inside compiler
    @get:Internal
    override val taskBuildCacheableOutputDirectory: DirectoryProperty
        get() = super.taskBuildCacheableOutputDirectory

    @get:Internal
    @get:Deprecated("Please use modeProperty instead.")
    var mode: KotlinJsBinaryMode
        get() = modeProperty.get()
        set(value) {
            modeProperty.set(value)
        }

    @get:Input
    internal abstract val modeProperty: Property<KotlinJsBinaryMode>

    @get:SkipWhenEmpty
    @get:IgnoreEmptyDirectories
    @get:NormalizeLineEndings
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    internal abstract val entryModule: DirectoryProperty

    // Do not change the visibility - the property could be used outside
    @get:Internal
    abstract val rootCacheDirectory: DirectoryProperty

    override fun cleanOutputsAndLocalState(reason: String?) {
        if (!usingCacheDirectory()) {
            super.cleanOutputsAndLocalState(reason)
        }
    }

    override fun isIncrementalCompilationEnabled(): Boolean = false

    override fun contributeAdditionalCompilerArguments(context: ContributeCompilerArgumentsContext<K2JSCompilerArguments>) {
        context.primitive { args ->
            args.irProduceJs = true

            // moduleName can start with @ for group of NPM packages
            // but args parsing @ as start of argfile
            // so WA we provide moduleName as one parameter
            if (args.moduleName != null) {
                args.freeArgs += "-ir-output-name=${args.moduleName}"
                args.moduleName = null
            }

            args.includes = entryModule.get().asFile.absolutePath

            if (usingCacheDirectory()) {
                args.cacheDirectory = rootCacheDirectory.get().asFile.also { it.mkdirs() }.absolutePath
            }

            if (isWasmPlatform && modeProperty.get() == DEVELOPMENT) {
                args.debuggerCustomFormatters = true
            }
        }
    }

    private val isWasmPlatform: Boolean =
        target == KotlinPlatformType.wasm

    override fun processArgsBeforeCompile(args: K2JSCompilerArguments) {
        if (!isWasmPlatform) {
            buildFusService.orNull?.reportFusMetrics {
                CompileKotlinJsIrLinkMetrics.collectMetrics(args, incrementalJsIr, it)
            }
        } else {
            buildFusService.orNull?.reportFusMetrics {
                CompileKotlinWasmIrLinkMetrics.collectMetrics(incrementalWasm, it)
            }
        }
    }

    private fun usingCacheDirectory() =
        (if (isWasmPlatform) incrementalWasm else incrementalJsIr) && modeProperty.get() == DEVELOPMENT
}
