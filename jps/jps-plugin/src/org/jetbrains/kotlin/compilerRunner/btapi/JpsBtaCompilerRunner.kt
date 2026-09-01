/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner.btapi

import com.intellij.util.xmlb.XmlSerializerUtil
import org.jetbrains.jps.incremental.CompileContext
import org.jetbrains.kotlin.buildtools.api.BaseCompilationOperation
import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.OperationCancelledException
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments
import org.jetbrains.kotlin.buildtools.api.getToolchain
import org.jetbrains.kotlin.buildtools.api.jvm.JvmClientManagedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.mergeBeans
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorUtil
import org.jetbrains.kotlin.compilerRunner.JpsCompilerEnvironment
import org.jetbrains.kotlin.compilerRunner.JpsKotlinCompilerRunner
import org.jetbrains.kotlin.compilerRunner.reportInternalCompilerError
import org.jetbrains.kotlin.config.CompilerSettings
import org.jetbrains.kotlin.incremental.components.EnumWhenTracker
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.incremental.components.ImportTracker
import org.jetbrains.kotlin.incremental.components.InlineConstTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.jps.build.KotlinBuilder
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.modules.TargetId
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Everything JPS put into `module.xml` on the legacy path, as plain values.
 *
 * The Build Tools API rejects `-Xbuild-file` (restricted since 2.5.0), so a chunk has to be described through
 * ordinary compiler arguments instead. That only works for a single-target chunk, which is why
 * `KotlinJvmModuleBuildTarget.compileModuleChunk` falls back to the legacy path for circular chunks.
 */
internal class JpsBtaJvmCompilationRequest(
    val targetId: TargetId,
    val sources: List<File>,
    val commonSources: List<File>,
    val outputDirectory: File,
    val classpathRoots: List<File>,
    val javaSourceRoots: List<File>,
    val friendDirectories: List<File>,
    val modularJdkRoot: File?,
)

/**
 * Compiles one JPS module chunk through the Build Tools API, in process, with client-managed incremental compilation.
 *
 * In process is not a preference but a requirement: `JvmCompilationOperationImpl.getIcOptionsOrNull` hard-fails when
 * a client-managed incremental configuration is used with the daemon execution policy.
 */
internal class JpsBtaCompilerRunner {
    fun runJvmCompilation(
        request: JpsBtaJvmCompilationRequest,
        commonArguments: CommonCompilerArguments,
        k2JvmArguments: K2JVMCompilerArguments,
        compilerSettings: CompilerSettings,
        environment: JpsCompilerEnvironment,
        buildSession: JpsBtaBuildSession,
        context: CompileContext,
    ) {
        try {
            val session = buildSession.getOrCreate(environment.kotlinPaths)
            val operation = buildOperation(session, request, commonArguments, k2JvmArguments, compilerSettings, environment)

            val result = withCancellationWatchdog(context, operation) {
                session.executeOperation(
                    operation,
                    session.kotlinToolchains.createInProcessExecutionPolicy(),
                    JpsBtaMessageCollectorLogger(environment.messageCollector),
                )
            }

            // Diagnostics already reached JPS through `JpsBtaMessageRenderer` -> `MessageCollectorAdapter` ->
            // `context.processMessage`, which is what sets `Utils.ERRORS_DETECTED_KEY`. But an OOM or an internal
            // compiler error can come back without any ERROR-severity message at all, so report those explicitly.
            if (result != CompilationResult.COMPILATION_SUCCESS && !environment.messageCollector.hasErrors()) {
                environment.messageCollector.report(
                    CompilerMessageSeverity.ERROR,
                    "Kotlin compilation of ${request.targetId.name} failed: $result"
                )
            }
        } catch (e: OperationCancelledException) {
            // Let JPS notice the cancellation itself through `context.checkCanceled()`.
            KotlinBuilder.LOG.info("Kotlin compilation of ${request.targetId.name} was cancelled", e)
        } catch (e: Throwable) {
            MessageCollectorUtil.reportException(environment.messageCollector, e)
            reportInternalCompilerError(environment.messageCollector)
        }
    }

    private fun buildOperation(
        session: KotlinToolchains.BuildSession,
        request: JpsBtaJvmCompilationRequest,
        commonArguments: CommonCompilerArguments,
        k2JvmArguments: K2JVMCompilerArguments,
        compilerSettings: CompilerSettings,
        environment: JpsCompilerEnvironment,
    ): JvmCompilationOperation {
        val jvm = session.kotlinToolchains.getToolchain<JvmPlatformToolchain>()
        val builder = jvm.jvmCompilationOperationBuilder(
            request.sources.map { it.toPath() },
            request.outputDirectory.toPath(),
        )

        applyCompilerArguments(builder, request, commonArguments, k2JvmArguments, compilerSettings)

        builder[JvmCompilationOperation.INCREMENTAL_COMPILATION] = buildIncrementalConfiguration(builder, request, environment)
        builder[BaseCompilationOperation.COMPILER_MESSAGE_RENDERER] = JpsBtaMessageRenderer(environment.messageCollector)

        return builder.build()
    }

    private fun applyCompilerArguments(
        builder: JvmCompilationOperation.Builder,
        request: JpsBtaJvmCompilationRequest,
        commonArguments: CommonCompilerArguments,
        k2JvmArguments: K2JVMCompilerArguments,
        compilerSettings: CompilerSettings,
    ) {
        val arguments = mergeBeans(commonArguments, XmlSerializerUtil.createCopy(k2JvmArguments))

        // Arguments the Build Tools API restricts. `-d` and `-Xbuild-file` are what `setupK2JvmArguments` would set
        // on the legacy path; the others can only come from the user's own compiler settings. Passing any of them
        // makes `applyArgumentStrings` record a violation that aborts the operation.
        arguments.destination = null
        arguments.buildFile = null
        arguments.expression = null
        arguments.includeRuntime = false
        arguments.incrementalCompilation = null

        // As `setupK2JvmArguments` does: JPS supplies the standard library and kotlin-reflect itself.
        arguments.noStdlib = true
        arguments.noReflect = true

        // The JDK needs more care than `setupK2JvmArguments`'s unconditional `noJdk = true`.
        //
        // On the module.xml path `configureSourceRoots` sets `JVMConfigurationKeys.JDK_HOME` from the module's
        // `modularJdkRoot` *regardless* of `-no-jdk`, so a modular JDK still reaches the compiler. Without a build
        // file there is no module to read that from, and `configureJdkHome` discards `-jdk-home` outright when
        // `-no-jdk` is set - which leaves `java.lang.Object` unresolvable.
        //
        // So: for a modular JDK, hand it over as `-jdk-home` and let the compiler pick it up (it adds no class roots
        // of its own for a modular JDK anyway). For a pre-9 JDK there is no `modularJdkRoot` and JPS puts the SDK
        // jars on the class path itself, which is exactly the `-no-jdk` case.
        arguments.jdkHome = arguments.jdkHome ?: request.modularJdkRoot?.path
        arguments.noJdk = arguments.jdkHome == null

        // A round where only sources were *removed* still has to run, so that the compiler rewrites
        // `META-INF/<module>.kotlin_module`. With `-Xbuild-file` the compiler allows that implicitly; without one it
        // reports "No source files" and produces nothing unless this is set.
        arguments.allowNoSourceFiles = true

        arguments.moduleName = request.targetId.name
        arguments.commonSources = request.commonSources.map { it.path }.toTypedArray()
        arguments.javaSourceRoots = (arguments.javaSourceRoots.toList() + request.javaSourceRoots.map { it.path })
            .distinct().toTypedArray()
        arguments.friendPaths = (arguments.friendPaths.toList() + request.friendDirectories.map { it.path })
            .distinct().toTypedArray()
        val classpath = (
                arguments.classpath?.split(File.pathSeparator).orEmpty() + request.classpathRoots.map { it.path }
                ).filter { it.isNotEmpty() }.distinct()
        arguments.classpath = classpath.takeIf { it.isNotEmpty() }?.joinToString(File.pathSeparator)

        // Arguments without a typed Build Tools API option (`-P`, `-Xplugin`, `-Xcommon-sources`, ...) are *dropped*
        // rather than *restricted*, and `applyArgumentStrings` parses into a full `K2JVMCompilerArguments` and copies
        // it back, so they still round-trip.
        val argumentStrings = JpsKotlinCompilerRunner().argumentStringsWithAdditional(arguments, compilerSettings)
        if (System.getProperty(DEBUG_ARGUMENTS_PROPERTY, "false").toBoolean()) {
            System.err.println("[BTA] ${request.targetId} args: " + argumentStrings.joinToString(" "))
        }
        builder.compilerArguments.applyArgumentStrings(argumentStrings)

        // The typed options below are set after `applyArgumentStrings` on purpose: the last write wins, and these
        // describe the chunk rather than the user's settings.
        with(builder.compilerArguments) {
            this[JvmCompilerArguments.X_ALLOW_NO_SOURCE_FILES] = true
            this[JvmCompilerArguments.MODULE_NAME] = request.targetId.name
            this[JvmCompilerArguments.NO_STDLIB] = true
            this[JvmCompilerArguments.NO_REFLECT] = true
            this[JvmCompilerArguments.NO_JDK] = arguments.noJdk
            this[JvmCompilerArguments.JDK_HOME] = arguments.jdkHome?.let { Paths.get(it) }
            this[JvmCompilerArguments.CLASSPATH] = classpath.map { Paths.get(it) }.ifEmpty { null }
            this[JvmCompilerArguments.X_JAVA_SOURCE_ROOTS] = arguments.javaSourceRoots.map { Paths.get(it) }
            this[JvmCompilerArguments.X_FRIEND_PATHS] = arguments.friendPaths.map { Paths.get(it) }
        }
    }

    private fun buildIncrementalConfiguration(
        builder: JvmCompilationOperation.Builder,
        request: JpsBtaJvmCompilationRequest,
        environment: JpsCompilerEnvironment,
    ): JvmClientManagedIncrementalCompilationConfiguration {
        val jpsComponents = environment.services[IncrementalCompilationComponents::class.java]
            ?: error("IncrementalCompilationComponents is not registered for ${request.targetId.name}")
        val cache = JpsBtaIncrementalCache(jpsComponents.getIncrementalCache(request.targetId))

        val icBuilder = builder.clientManagedIcConfigurationBuilder(
            JpsBtaSingleTargetIncrementalCompilationComponents(cache)
        )

        // JPS keeps its own tracker instances: `updateChunkMappings`, `updateCaches` and `updateLookupStorage` all
        // read them back and downcast to the concrete `*Impl` types afterwards.
        environment.services[LookupTracker::class.java]?.let {
            icBuilder[JvmClientManagedIncrementalCompilationConfiguration.LOOKUP_TRACKER] = JpsBtaLookupTracker(it)
        }
        environment.services[ExpectActualTracker::class.java]?.let {
            icBuilder[JvmClientManagedIncrementalCompilationConfiguration.EXPECT_ACTUAL_TRACKER] =
                JpsBtaExpectActualTracker(it)
        }
        environment.services[InlineConstTracker::class.java]?.let {
            icBuilder[JvmClientManagedIncrementalCompilationConfiguration.INLINE_CONST_TRACKER] =
                JpsBtaInlineConstTracker(it)
        }
        environment.services[EnumWhenTracker::class.java]?.let {
            icBuilder[JvmClientManagedIncrementalCompilationConfiguration.ENUM_WHEN_TRACKER] = JpsBtaEnumWhenTracker(it)
        }
        environment.services[ImportTracker::class.java]?.let {
            icBuilder[JvmClientManagedIncrementalCompilationConfiguration.IMPORT_TRACKER] = JpsBtaImportTracker(it)
        }
        icBuilder[JvmClientManagedIncrementalCompilationConfiguration.FILE_MAPPING_TRACKER] =
            JpsBtaFileMappingTracker(environment.outputItemsCollector)

        return icBuilder.build()
    }

    /**
     * The Build Tools API only offers push cancellation ([JvmCompilationOperation.cancel]), while JPS only offers a
     * pull-style [CompileContext.getCancelStatus]. Bridge the two with a poller for the duration of the operation.
     */
    private fun <R> withCancellationWatchdog(
        context: CompileContext,
        operation: JvmCompilationOperation,
        body: () -> R,
    ): R {
        val finished = AtomicBoolean(false)
        val watchdog = Thread {
            while (!finished.get()) {
                if (context.cancelStatus.isCanceled) {
                    operation.cancel()
                    return@Thread
                }
                try {
                    Thread.sleep(CANCELLATION_POLL_INTERVAL_MS)
                } catch (_: InterruptedException) {
                    return@Thread
                }
            }
        }
        watchdog.isDaemon = true
        watchdog.name = "Kotlin JPS BTA cancellation watchdog"
        watchdog.start()

        return try {
            body()
        } finally {
            finished.set(true)
            watchdog.interrupt()
        }
    }

    private companion object {
        const val CANCELLATION_POLL_INTERVAL_MS = 200L

        /** Set to `true` to print the compiler argument strings of every chunk. Spike diagnostics only. */
        const val DEBUG_ARGUMENTS_PROPERTY = "kotlin.jps.btaDebugArguments"
    }
}
