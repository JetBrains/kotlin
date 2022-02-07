/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.*
import org.gradle.internal.hash.FileHasher
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.compilerRunner.ArgumentUtils
import org.jetbrains.kotlin.gradle.dsl.KotlinJsOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJsOptionsImpl
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinCompilationData
import org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsService
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode.DEVELOPMENT
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode.PRODUCTION
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.utils.getValue
import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics
import org.jetbrains.kotlin.statistics.metrics.StringMetrics
import java.io.File
import javax.inject.Inject

@CacheableTask
abstract class KotlinJsIrLink @Inject constructor(
    objectFactory: ObjectFactory,
    workerExecutor: WorkerExecutor
) : Kotlin2JsCompile(
    KotlinJsOptionsImpl(),
    objectFactory,
    workerExecutor
) {

    class Configurator(compilation: KotlinCompilationData<*>) : Kotlin2JsCompile.Configurator<KotlinJsIrLink>(compilation) {

        override fun configure(task: KotlinJsIrLink) {
            super.configure(task)

            task.entryModule.fileProvider(
                (compilation as KotlinJsIrCompilation).output.classesDirs.elements.map { it.single().asFile }
            ).disallowChanges()
            task.destinationDirectory.fileProvider(task.outputFileProperty.map { it.parentFile }).disallowChanges()
        }
    }

    @Transient
    @get:Internal
    internal lateinit var compilation: KotlinCompilationData<*>

    private val platformType by project.provider {
        compilation.platformType
    }

    @Transient
    @get:Internal
    internal val propertiesProvider = PropertiesProvider(project)

    @get:Inject
    open val fileHasher: FileHasher
        get() = throw UnsupportedOperationException()

    @get:Input
    internal val incrementalJsIr: Boolean = propertiesProvider.incrementalJsIr

    @get:Input
    val outputGranularity: KotlinJsIrOutputGranularity = propertiesProvider.jsIrOutputGranularity

    // Link tasks are not affected by compiler plugin
    override val pluginClasspath: ConfigurableFileCollection = project.objects.fileCollection()

    @Input
    lateinit var mode: KotlinJsBinaryMode

    // Not check sources, only klib module
    @Internal
    override fun getSource(): FileTree = super.getSource()

    private val buildDir = project.buildDir

    @get:SkipWhenEmpty
    @get:IgnoreEmptyDirectories
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    internal abstract val entryModule: DirectoryProperty

    override fun getDestinationDir(): File {
        return if (kotlinOptions.outputFile == null) {
            super.getDestinationDir()
        } else {
            outputFile.parentFile
        }
    }

    override fun skipCondition(): Boolean {
        return !entryModule.get().asFile.exists()
    }

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
        if (incrementalJsIr && mode == DEVELOPMENT) {
            args.cacheDirectories = args.libraries?.splitByPathSeparator()
                ?.map {
                    val file = File(it)
                    rootCacheDirectory
                        .resolve(file.nameWithoutExtension)
                        .also {
                            it.mkdirs()
                        }
                        .resolve(fileHasher.hash(file).toString())
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

    override fun setupCompilerArgs(args: K2JSCompilerArguments, defaultsOnly: Boolean, ignoreClasspathResolutionErrors: Boolean) {
        when (mode) {
            PRODUCTION -> {
                kotlinOptions.configureOptions(ENABLE_DCE, GENERATE_D_TS)
            }
            DEVELOPMENT -> {
                kotlinOptions.configureOptions(GENERATE_D_TS)
            }
        }
        val alreadyDefinedOutputMode = kotlinOptions.freeCompilerArgs
            .any { it.startsWith(PER_MODULE) }
        if (!alreadyDefinedOutputMode) {
            kotlinOptions.freeCompilerArgs += outputGranularity.toCompilerArgument()
        }
        super.setupCompilerArgs(args, defaultsOnly, ignoreClasspathResolutionErrors)
    }

    private fun KotlinJsOptions.configureOptions(vararg additionalCompilerArgs: String) {
        freeCompilerArgs += additionalCompilerArgs.toList() +
                PRODUCE_JS +
                "$ENTRY_IR_MODULE=${entryModule.get().asFile.canonicalPath}"

        if (platformType == KotlinPlatformType.wasm) {
            freeCompilerArgs += WASM_BACKEND
        }
    }
}