/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.typescript

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.work.Incremental
import org.gradle.work.NormalizeLineEndings
import org.jetbrains.kotlin.buildtools.api.js.JsDtsGranularity
import org.jetbrains.kotlin.buildtools.api.js.JsPlatformToolchain.Companion.js
import org.jetbrains.kotlin.buildtools.api.js.operations.JsDtsGenerationOperation.Companion.COMPILE_LONG_AS_BIG_INT
import org.jetbrains.kotlin.buildtools.api.js.operations.JsDtsGenerationOperation.Companion.DATA_CLASS_COPY_RESPECTS_CONSTRUCTOR_VISIBILITY
import org.jetbrains.kotlin.buildtools.api.js.operations.JsDtsGenerationOperation.Companion.EXPORT_SUSPEND_LAMBDAS
import org.jetbrains.kotlin.buildtools.api.js.operations.JsDtsGenerationOperation.Companion.EXPORT_UNTYPED_AS_UNKNOWN
import org.jetbrains.kotlin.buildtools.api.js.operations.JsDtsGenerationOperation.Companion.GRANULARITY
import org.jetbrains.kotlin.buildtools.api.js.operations.JsDtsGenerationOperation.Companion.IMPLEMENT_INTERFACES
import org.jetbrains.kotlin.buildtools.api.js.operations.JsDtsGenerationOperation.Companion.MODULE_KIND
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.compilerRunner.btapi.UsesBuildSessionService
import org.jetbrains.kotlin.gradle.dsl.JsModuleKind
import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompilerOptionsDefault
import org.jetbrains.kotlin.gradle.internal.UsesClassLoadersCachingBuildService
import org.jetbrains.kotlin.gradle.logging.GradleKotlinLogger
import org.jetbrains.kotlin.gradle.logging.kotlinDebug
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrOutputGranularity
import java.io.File
import javax.inject.Inject
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JsModuleKind as BtaJsModuleKind

@CacheableTask
internal abstract class KotlinJsDtsGenerationTask @Inject constructor(
    objectFactory: ObjectFactory,
) : DefaultTask(), UsesClassLoadersCachingBuildService, UsesBuildSessionService {
    @get:Internal
    abstract val klibs: ConfigurableFileCollection

    @get:Input
    abstract val granularity: Property<KotlinJsIrOutputGranularity>

    @get:SkipWhenEmpty
    @get:IgnoreEmptyDirectories
    @get:NormalizeLineEndings
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val entryModule: DirectoryProperty

    @get:Nested
    val linkCompilerOptions: KotlinJsCompilerOptions = objectFactory.newInstance(
        KotlinJsCompilerOptionsDefault::class.java,
        objectFactory,
    )

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Classpath
    internal abstract val kotlinBuildToolsApiClasspath: ConfigurableFileCollection


    @TaskAction
    fun run() {
        if (!entryModule.get().asFile.exists()) {
            return
        }

        val buildSession = buildSessionService.get().getOrCreateBuildSession(
            classLoadersCachingService.get(),
            kotlinBuildToolsApiClasspath.toList()
        )

        val dtsOperation = buildSession.kotlinToolchains.js.jsDtsGenerationOperationBuilder(
            klibs = (klibs.files + entryModule.get().asFile).map(File::toPath),
            outputDirectory = outputDirectory.get().asFile.toPath(),
        )
            .apply {
                val moduleKind = linkCompilerOptions.moduleKind.orNull ?: JsModuleKind.MODULE_UMD
                val compilerArguments = linkCompilerOptions.freeCompilerArgs.get()

                this[MODULE_KIND] = BtaJsModuleKind.values().single { it.stringValue == moduleKind.kind }
                this[GRANULARITY] = when (granularity.get()) {
                    KotlinJsIrOutputGranularity.PER_FILE -> JsDtsGranularity.PER_FILE
                    else -> JsDtsGranularity.WHOLE_PROGRAM
                }

                this[COMPILE_LONG_AS_BIG_INT] = K2JSCompilerArguments::compileLongAsBigInt.cliArgument in compilerArguments
                this[IMPLEMENT_INTERFACES] = K2JSCompilerArguments::allowImplementableInterfacesExporting.cliArgument in compilerArguments
                this[EXPORT_SUSPEND_LAMBDAS] = K2JSCompilerArguments::allowExportingSuspendLambdas.cliArgument in compilerArguments
                this[EXPORT_UNTYPED_AS_UNKNOWN] = K2JSCompilerArguments::useUnknownInsteadAny.cliArgument in compilerArguments
                this[DATA_CLASS_COPY_RESPECTS_CONSTRUCTOR_VISIBILITY] =
                    CommonCompilerArguments::consistentDataClassCopyVisibility.cliArgument in compilerArguments
            }
            .build()

        buildSession.executeOperation(
            dtsOperation,
            buildSession.kotlinToolchains.createInProcessExecutionPolicy(),
            GradleKotlinLogger(logger)
        )
    }

    companion object {
        const val NAME = "generateTypeScriptDefinitions"
        const val OUTPUT_DIRECTORY_NAME = "typescript-definitions"
    }
}
