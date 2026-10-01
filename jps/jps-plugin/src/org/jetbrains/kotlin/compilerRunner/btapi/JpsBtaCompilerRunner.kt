/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner.btapi

import com.intellij.util.xmlb.XmlSerializerUtil
import org.jetbrains.jps.incremental.CompileContext
import org.jetbrains.kotlin.build.JvmSourceRoot
import org.jetbrains.kotlin.buildtools.api.BaseCompilationOperation
import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.OperationCancelledException
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments
import org.jetbrains.kotlin.buildtools.api.getToolchain
import org.jetbrains.kotlin.buildtools.api.jps.jvm.JvmJpsManagedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.jps.jvm.operations.jpsManagedIcConfigurationBuilder
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

internal class JpsBtaJvmCompilationRequest(
    val targetId: TargetId,
    val sources: List<File>,
    val commonSources: List<File>,
    val outputDirectory: File,
    val classpathRoots: List<File>,
    val javaSourceRoots: List<JvmSourceRoot>,
    val friendDirectories: List<File>,
    val modularJdkRoot: File?,
)

internal class JpsBtaCompilerRunner {
    fun runJvmCompilation(
        request: JpsBtaJvmCompilationRequest,
        commonArguments: CommonCompilerArguments,
        k2JvmArguments: K2JVMCompilerArguments,
        compilerSettings: CompilerSettings,
        environment: JpsCompilerEnvironment,
        session: KotlinToolchains.BuildSession,
        context: CompileContext,
    ) {
        try {
            val operation = buildOperation(session, request, commonArguments, k2JvmArguments, compilerSettings, environment)

            val result = withCancellationWatchdog(context, operation) {
                session.executeOperation(
                    operation,
                    session.kotlinToolchains.createInProcessExecutionPolicy(),
                    JpsBtaMessageCollectorLogger(environment.messageCollector),
                )
            }

            // A failure like OOM or an internal compiler error may come without any reported error
            if (result != CompilationResult.COMPILATION_SUCCESS && !environment.messageCollector.hasErrors()) {
                environment.messageCollector.report(
                    CompilerMessageSeverity.ERROR,
                    "Kotlin compilation of ${request.targetId.name} failed: $result"
                )
            }
        } catch (e: OperationCancelledException) {
            KotlinBuilder.LOG.info("Kotlin compilation of ${request.targetId.name} was cancelled", e)
            context.checkCanceled()
            throw e
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

        // Restricted by the Build Tools API: passing any of them fails the operation
        arguments.destination = null
        arguments.buildFile = null
        arguments.expression = null
        arguments.includeRuntime = false
        arguments.incrementalCompilation = null

        // As in `setupK2JvmArguments`: JPS supplies the standard library and kotlin-reflect itself
        arguments.noStdlib = true
        arguments.noReflect = true

        // Unlike `setupK2JvmArguments`, `-no-jdk` can't be set unconditionally: without a module.xml the modular JDK
        // is passed as `-jdk-home`, which the compiler ignores together with `-no-jdk`.
        // A pre-9 JDK has no `modularJdkRoot`, its jars are already on the class path.
        arguments.jdkHome = arguments.jdkHome ?: request.modularJdkRoot?.path
        arguments.noJdk = arguments.jdkHome == null

        // A round with only removed sources still has to rewrite `META-INF/<module>.kotlin_module`
        arguments.allowNoSourceFiles = true

        arguments.moduleName = request.targetId.name
        arguments.commonSources = request.commonSources.map { it.path }.toTypedArray()
        arguments.javaSourceRoots = (arguments.javaSourceRoots.toList() + request.javaSourceRoots.map { it.file.path })
            .distinct().toTypedArray()
        arguments.friendPaths = (arguments.friendPaths.toList() + request.friendDirectories.map { it.path })
            .distinct().toTypedArray()
        val classpath = (
                arguments.classpath?.split(File.pathSeparator).orEmpty() + request.classpathRoots.map { it.path }
                ).filter { it.isNotEmpty() }.distinct()
        arguments.classpath = classpath.takeIf { it.isNotEmpty() }?.joinToString(File.pathSeparator)

        // String arguments keep the options without a typed Build Tools API counterpart (`-P`, `-Xplugin`, ...)
        val argumentStrings = JpsKotlinCompilerRunner().argumentStringsWithAdditional(arguments, compilerSettings)
        if (KotlinBuilder.LOG.isDebugEnabled) {
            KotlinBuilder.LOG.debug("Build Tools API arguments for ${request.targetId.name}: ${argumentStrings.joinToString(" ")}")
        }
        @Suppress("DEPRECATION")
        builder.compilerArguments.applyArgumentStrings(argumentStrings)

        // Set after `applyArgumentStrings` to take precedence over the additional arguments from the user settings
        with(builder.compilerArguments) {
            this[JvmCompilerArguments.X_ALLOW_NO_SOURCE_FILES] = true
            this[JvmCompilerArguments.MODULE_NAME] = request.targetId.name
            this[JvmCompilerArguments.NO_STDLIB] = true
            this[JvmCompilerArguments.NO_REFLECT] = true
            this[JvmCompilerArguments.X_JAVA_PACKAGE_PREFIX] = request.javaSourceRoots.map { it.packagePrefix }.singleOrNull()
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
    ): JvmJpsManagedIncrementalCompilationConfiguration {
        val jpsComponents = environment.services[IncrementalCompilationComponents::class.java]
            ?: error("IncrementalCompilationComponents is not registered for ${request.targetId.name}")
        val cache = JpsBtaIncrementalCache(jpsComponents.getIncrementalCache(request.targetId))

        val icBuilder = builder.jpsManagedIcConfigurationBuilder(
            JpsBtaSingleTargetIncrementalCompilationComponents(cache)
        )

        // JPS reads its own trackers back after the compilation, so they are wrapped rather than replaced
        environment.services[LookupTracker::class.java]?.let {
            icBuilder[JvmJpsManagedIncrementalCompilationConfiguration.LOOKUP_TRACKER] = JpsBtaLookupTracker(it)
        }
        environment.services[ExpectActualTracker::class.java]?.let {
            icBuilder[JvmJpsManagedIncrementalCompilationConfiguration.EXPECT_ACTUAL_TRACKER] =
                JpsBtaExpectActualTracker(it)
        }
        environment.services[InlineConstTracker::class.java]?.let {
            icBuilder[JvmJpsManagedIncrementalCompilationConfiguration.INLINE_CONST_TRACKER] =
                JpsBtaInlineConstTracker(it)
        }
        environment.services[EnumWhenTracker::class.java]?.let {
            icBuilder[JvmJpsManagedIncrementalCompilationConfiguration.ENUM_WHEN_TRACKER] = JpsBtaEnumWhenTracker(it)
        }
        environment.services[ImportTracker::class.java]?.let {
            icBuilder[JvmJpsManagedIncrementalCompilationConfiguration.IMPORT_TRACKER] = JpsBtaImportTracker(it)
        }
        icBuilder[JvmJpsManagedIncrementalCompilationConfiguration.FILE_MAPPING_TRACKER] =
            JpsBtaFileMappingTracker(environment.outputItemsCollector)

        return icBuilder.build()
    }

    /**
     * Polls [CompileContext.getCancelStatus] and forwards the cancellation to [JvmCompilationOperation.cancel].
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
    }
}
