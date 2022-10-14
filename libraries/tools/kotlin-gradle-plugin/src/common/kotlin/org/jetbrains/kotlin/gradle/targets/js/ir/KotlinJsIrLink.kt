/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.NormalizeLineEndings
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.compilerRunner.ArgumentUtils
import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompilerOptionsDefault
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsService
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode.DEVELOPMENT
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.utils.toHexString
import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics
import org.jetbrains.kotlin.statistics.metrics.StringMetrics
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.inject.Inject

@CacheableTask
abstract class KotlinJsIrLink @Inject constructor(
    objectFactory: ObjectFactory,
    workerExecutor: WorkerExecutor
) : Kotlin2JsCompile(
    objectFactory.newInstance(KotlinJsCompilerOptionsDefault::class.java),
    objectFactory,
    workerExecutor
) {

    init {
        // Not check sources, only klib module
        disallowSourceChanges()
    }

    @get:Internal
    override val sources: FileCollection = super.sources

    override fun skipCondition(): Boolean {
        return !entryModule.get().asFile.exists()
    }

    @get:Internal
    val platformType by lazy {
        compilation.platformType
    }

    @Transient
    @get:Internal
    internal lateinit var compilation: KotlinCompilation<*>

    @Transient
    @get:Internal
    internal val propertiesProvider = PropertiesProvider(project)

    @get:Input
    internal val incrementalJsIr: Boolean = propertiesProvider.incrementalJsIr

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

    private val buildDir = project.buildDir

    @get:SkipWhenEmpty
    @get:IgnoreEmptyDirectories
    @get:NormalizeLineEndings
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    internal abstract val entryModule: DirectoryProperty

    @get:Internal
    val rootCacheDirectory by lazy {
        buildDir.resolve("klib/cache")
    }

    override fun processArgs(args: K2JSCompilerArguments) {
        super.processArgs(args)
        KotlinBuildStatsService.applyIfInitialised {
            it.report(BooleanMetrics.JS_IR_INCREMENTAL, incrementalJsIr)
            val newArgs = K2JSCompilerArguments()
            parseCommandLineArguments(ArgumentUtils.convertArgumentsToStringList(args), newArgs)
            it.report(
                StringMetrics.JS_OUTPUT_GRANULARITY,
                if (newArgs.irPerModule)
                    KotlinJsIrOutputGranularity.PER_MODULE.name.toLowerCase()
                else
                    KotlinJsIrOutputGranularity.WHOLE_PROGRAM.name.toLowerCase()
            )
        }

        args.includes = entryModule.get().asFile.canonicalPath

        if (incrementalJsIr && mode == DEVELOPMENT) {
            val digest = MessageDigest.getInstance("SHA-256")
            args.cacheDirectories = args.libraries?.splitByPathSeparator()
                ?.map {
                    val file = File(it)
                    val hash = digest.digest(file.normalize().absolutePath.toByteArray(StandardCharsets.UTF_8)).toHexString()
                    rootCacheDirectory
                        .resolve(file.nameWithoutExtension)
                        .resolve(hash)
                        .also {
                            it.mkdirs()
                        }
                }
                ?.plus(rootCacheDirectory.resolve(entryModule.get().asFile.name))
                ?.let {
                    if (it.isNotEmpty())
                        it.joinToString(File.pathSeparator)
                    else
                        null
                }
        }
    }

    private fun String.splitByPathSeparator(): List<String> {
        return this.split(File.pathSeparator.toRegex())
            .dropLastWhile { it.isEmpty() }
            .toTypedArray()
            .filterNot { it.isEmpty() }
    }
}

val KotlinPlatformType.fileExtension
    get() = when (this) {
        KotlinPlatformType.wasm -> {
            ".mjs"
        }

        KotlinPlatformType.js -> {
            ".js"
        }

        else -> error("Only JS and WASM supported for KotlinJsTest")
    }