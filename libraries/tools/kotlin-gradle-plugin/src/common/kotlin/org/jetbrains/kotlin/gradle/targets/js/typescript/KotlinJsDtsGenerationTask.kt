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
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.work.NormalizeLineEndings
import org.jetbrains.kotlin.buildtools.api.DelicateBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.js.JsDtsGranularity
import org.jetbrains.kotlin.buildtools.api.js.JsPlatformToolchain.Companion.js
import org.jetbrains.kotlin.buildtools.api.js.operations.JsDtsGenerationOperation.Companion.GRANULARITY
import org.jetbrains.kotlin.buildtools.api.js.operations.JsLinkingOperation
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.compilerRunner.btapi.UsesBuildSessionService
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings
import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompilerOptionsHelper
import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompilerOptionsDefault
import org.jetbrains.kotlin.gradle.internal.UsesClassLoadersCachingBuildService
import org.jetbrains.kotlin.gradle.logging.GradleKotlinLogger
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.UsesKotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrOutputGranularity
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.jetbrains.kotlin.tooling.core.toKotlinVersion
import java.io.File
import javax.inject.Inject

@CacheableTask
internal abstract class KotlinJsDtsGenerationTask @Inject constructor(
    objectFactory: ObjectFactory,
) : DefaultTask(), UsesClassLoadersCachingBuildService, UsesBuildSessionService, UsesKotlinToolingDiagnostics {
    init {
        onlyIf("TypeScript generating is skipped (the same as the KotlinJsIrLink task) in case of the entry klib doesn't exist") {
            (it as KotlinJsDtsGenerationTask).entryModule.get().asFile.exists()
        }
    }

    @get:Classpath
    @get:NormalizeLineEndings
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
    @get:NormalizeLineEndings
    internal abstract val kotlinBuildToolsApiClasspath: ConfigurableFileCollection


    @TaskAction
    fun run() {
        val buildSession = buildSessionService.get().getOrCreateBuildSession(
            classLoadersCachingService.get(),
            kotlinBuildToolsApiClasspath.toList()
        )

        val buildToolsApiVersion = buildSession.kotlinToolchains.getCompilerVersion()
        if (KotlinToolingVersion(buildToolsApiVersion).toKotlinVersion() < MINIMAL_SUPPORTED_BUILD_TOOLS_API_VERSION) {
            reportDiagnostic(
                KotlinToolingDiagnostics.RichTypeScriptDeclarationsUnsupportedBuildToolsApiVersion(
                    buildToolsApiVersion,
                    MINIMAL_SUPPORTED_BUILD_TOOLS_API_VERSION.toString()
                )
            )
            return
        }

        val dtsOperation = buildSession.kotlinToolchains.js.jsDtsGenerationOperationBuilder(
            klibs = (klibs.files + entryModule.get().asFile).map(File::toPath),
            outputDirectory = outputDirectory.get().asFile.toPath(),
        )
            .apply {
                // Let BTA derive all the settings from the link compiler arguments the same way the compiler does it
                // (defaults implied by the target, language features enabled via `-XXLanguage:` or `-language-version`, etc.)
                configureFrom(createLinkingOperation(buildSession.kotlinToolchains))
                this[GRANULARITY] = when (granularity.get()) {
                    KotlinJsIrOutputGranularity.PER_FILE -> JsDtsGranularity.PER_FILE
                    else -> JsDtsGranularity.WHOLE_PROGRAM
                }
            }
            .build()

        buildSession.executeOperation(
            dtsOperation,
            buildSession.kotlinToolchains.createInProcessExecutionPolicy(),
            GradleKotlinLogger(logger)
        )
    }

    private fun createLinkingOperation(kotlinToolchains: KotlinToolchains): JsLinkingOperation {
        val linkArguments = K2JSCompilerArguments()
        KotlinJsCompilerOptionsHelper.fillCompilerArguments(linkCompilerOptions, linkArguments)

        return kotlinToolchains.js.jsLinkingOperationBuilder(
            klib = entryModule.get().asFile.toPath(),
            destination = outputDirectory.get().asFile.toPath(),
        )
            .apply {
                @OptIn(DelicateBuildToolsApi::class)
                compilerArguments.applyCommandLineArguments(linkArguments.toArgumentStrings(allowArgFileInValues = false))
            }
            .build()
    }

    companion object {
        const val NAME = "generateTypeScriptDefinitions"
        const val OUTPUT_DIRECTORY_NAME = "typescript-definitions"

        private val MINIMAL_SUPPORTED_BUILD_TOOLS_API_VERSION = KotlinVersion(2, 5, 0)
    }
}
