/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalBuildToolsApi::class)

package org.jetbrains.kotlin.compilerRunner.btapi

import org.jetbrains.jps.incremental.CompileContext
import org.jetbrains.kotlin.buildtools.api.BaseCompilationOperation
import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.BaseIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorUtil
import org.jetbrains.kotlin.compilerRunner.JpsCompilerEnvironment
import org.jetbrains.kotlin.compilerRunner.JpsKotlinLogger
import org.jetbrains.kotlin.compilerRunner.reportInternalCompilerError
import org.jetbrains.kotlin.compilerRunner.withProgressReporter
import org.jetbrains.kotlin.config.CompilerSettings
import org.jetbrains.kotlin.jps.build.KotlinBuilder

/**
 * Compiles one JPS module through the Build Tools API, replacing the `module.xml` plus compile daemon machinery of
 * [org.jetbrains.kotlin.compilerRunner.JpsKotlinCompilerRunner].
 *
 * Only Kotlin-only modules and in-process execution are supported; the seam in
 * `KotlinJvmModuleBuildTarget.compileModuleChunk` rejects everything else before this runner is reached.
 *
 * Incremental compilation, when enabled, is run by the compiler rather than by JPS; see
 * [configureIncrementalCompilation].
 */
internal class JpsBuildToolsApiCompilerRunner(
    private val context: CompileContext,
    private val environment: JpsCompilerEnvironment,
) {
    private val log: KotlinLogger = JpsBtaLogger(JpsKotlinLogger(KotlinBuilder.LOG), environment.messageCollector, isVerbose)

    companion object {
        const val USE_BUILD_TOOLS_API_PROPERTY = "kotlin.jps.useBuildToolsApi"

        const val VERBOSE_PROPERTY = "kotlin.jps.verbose"

        /**
         * In the IDE this is set per project under
         * *Settings | Build, Execution, Deployment | Compiler | Shared build process VM options*.
         */
        val isEnabled: Boolean
            get() = System.getProperty(USE_BUILD_TOOLS_API_PROPERTY).toBoolean()

        /**
         * The equivalent of Gradle's `--info`/`--debug` for this path: everything the path would otherwise write to
         * the build process log is reported to the *Build* tool window as well, see [JpsBtaLogger].
         *
         * Set next to [USE_BUILD_TOOLS_API_PROPERTY], in the same *Shared build process VM options*. Affects the Build
         * Tools API path only; the legacy path has its own `-verbose` compiler argument.
         */
        val isVerbose: Boolean
            get() = System.getProperty(VERBOSE_PROPERTY).toBoolean()
    }

    /**
     * @return whether a compilation was actually run, which is what `compileModuleChunk` reports back to JPS.
     * Compile errors are not a `false`: they reach JPS through [JpsCompilerMessageRendererBridge] and make
     * `KotlinBuilder` abort the build on its own.
     */
    fun compile(
        unit: JpsBtaCompilationUnit,
        commonArguments: CommonCompilerArguments,
        moduleArguments: K2JVMCompilerArguments,
        compilerSettings: CompilerSettings,
    ): Boolean {
        try {
            // The toolchains come from the build rather than from the provider directly, so that every chunk of one
            // build uses the same ones as the session it executes on; see [JpsBtaBuild].
            val build = context.getOrCreateBtaBuild(
                loadToolchains = { JpsBtaToolchainsProvider.getToolchains(environment.kotlinPaths, log) },
                onCreated = { report("Build session started, compiler version ${it.toolchains.getCompilerVersion()}") },
            )
            val kotlinToolchains = build.toolchains

            val argumentStrings = unit.toCompilerArgumentStrings(commonArguments, moduleArguments, compilerSettings)
            logCompilationInput(unit, argumentStrings)

            val renderer = JpsCompilerMessageRendererBridge(environment.messageCollector, environment.outputItemsCollector, isVerbose)
            val operation = kotlinToolchains.jvm
                .jvmCompilationOperationBuilder(unit.sources.map { it.toPath() }, unit.outputDir.toPath())
                .apply {
                    this[BaseCompilationOperation.COMPILER_MESSAGE_RENDERER] = renderer
                    compilerArguments.applyArgumentStrings(argumentStrings)
                    unit.incremental?.let { configureIncrementalCompilation(it) }
                }
                .build()

            val startNanos = System.nanoTime()
            val result = environment.withProgressReporter { progress ->
                progress.compilationStarted()
                build.session.executeOperation(operation, kotlinToolchains.createInProcessExecutionPolicy(), log)
            }
            val elapsedMs = (System.nanoTime() - startNanos) / 1_000_000

            report(
                "Compiled '${unit.moduleName}' in $elapsedMs ms: $result, " +
                        "${renderer.compiledSources.size} of ${unit.sources.size} sources, " +
                        "${renderer.outputFileCount} output files"
            )
            if (log.isDebugEnabled) {
                log.debug(
                    "[${unit.moduleName}] recompiled: " +
                            renderer.compiledSources.joinToString(separator = "\n  ", prefix = "\n  ")
                )
            }
            reportFailureWithoutDiagnostic(result)
        } catch (e: Throwable) {
            MessageCollectorUtil.reportException(environment.messageCollector, e)
            reportInternalCompilerError(environment.messageCollector)
        }

        return true
    }

    /**
     * Hands incremental compilation to the compiler, which keeps its own caches under
     * [JpsBtaIncrementalCompilation.workingDir] and works out the compile set itself from the changes JPS noticed.
     *
     * No classpath snapshots are passed and classpath comparison is switched off: modules that depend on other modules
     * are rejected at the seam, so the only classpath entries left are libraries and the SDK, and a change in those
     * goes through JPS's own cache invalidation instead.
     *
     * The very first build after the caches are gone is necessarily a full one: the compiler has no previous classpath
     * snapshot to compare against and falls back to compiling everything, writing the snapshot on the way out.
     */
    private fun JvmCompilationOperation.Builder.configureIncrementalCompilation(incremental: JpsBtaIncrementalCompilation) {
        val configuration = snapshotBasedIcConfigurationBuilder(
            workingDirectory = incremental.workingDir.toPath(),
            sourcesChanges = incremental.sourcesChanges,
            dependenciesSnapshotFiles = emptyList(),
        ).apply {
            this[JvmSnapshotBasedIncrementalCompilationConfiguration.ASSURED_NO_CLASSPATH_SNAPSHOT_CHANGES] = true
            this[BaseIncrementalCompilationConfiguration.FORCE_RECOMPILATION] = incremental.forceRecompilation
            // These two go together: class files overwritten by a failed compilation are restored, and the caches are
            // only flushed once it succeeded, so a failure cannot leave the caches describing outputs that were rolled
            // back. Correctness over speed, since JPS no longer keeps a second copy of that state.
            this[BaseIncrementalCompilationConfiguration.BACKUP_CLASSES] = true
            this[BaseIncrementalCompilationConfiguration.KEEP_IC_CACHES_IN_MEMORY] = true
        }.build()

        this[JvmCompilationOperation.INCREMENTAL_COMPILATION] = configuration
    }

    /**
     * Reports progress of the Build Tools API path itself, as opposed to compiler diagnostics.
     *
     * `INFO` rather than [log], because [org.jetbrains.kotlin.jps.build.MessageCollectorAdapter] turns it into a
     * `BuildMessage.Kind.INFO` that shows up in the *Build* tool window, while the logger only reaches the build
     * process log file. The tag keeps these lines distinguishable from the compiler's own output.
     */
    private fun report(message: String) {
        environment.messageCollector.report(CompilerMessageSeverity.INFO, "[Build Tools API] $message")
    }

    /**
     * The detail that would drown the *Build* tool window goes to the build process log instead. Enable it either with
     * the `#org.jetbrains.kotlin.jps.build.KotlinBuilder` category at `FINER` in `build-log-jul.properties`, or with
     * [VERBOSE_PROPERTY], which also brings it back into the *Build* tool window.
     */
    private fun logCompilationInput(unit: JpsBtaCompilationUnit, argumentStrings: List<String>) {
        report(
            "Compiling '${unit.moduleName}': ${unit.sources.size} sources, " +
                    "${unit.classpath.size} classpath entries, output ${unit.outputDir}"
        )
        if (!log.isDebugEnabled) return

        log.debug("[${unit.moduleName}] sources: ${unit.sources.joinToString(separator = "\n  ", prefix = "\n  ")}")
        log.debug("[${unit.moduleName}] classpath: ${unit.classpath.joinToString(separator = "\n  ", prefix = "\n  ")}")
        log.debug("[${unit.moduleName}] arguments: ${argumentStrings.joinToString(" ")}")
    }

    /**
     * A compilation can fail without any `ERROR` message having been reported, in which case nothing would make JPS
     * fail the build. Report one so that the failure is not silently swallowed.
     */
    private fun reportFailureWithoutDiagnostic(result: CompilationResult) {
        when (result) {
            CompilationResult.COMPILATION_SUCCESS, CompilationResult.COMPILATION_ERROR -> return
            CompilationResult.COMPILATION_OOM_ERROR ->
                environment.messageCollector.report(CompilerMessageSeverity.ERROR, "Compiler ran out of memory")
            CompilationResult.COMPILER_INTERNAL_ERROR -> reportInternalCompilerError(environment.messageCollector)
        }
    }
}
