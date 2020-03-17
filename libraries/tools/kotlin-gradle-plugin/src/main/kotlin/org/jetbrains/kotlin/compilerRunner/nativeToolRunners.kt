/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner

import org.gradle.api.Project
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.gradle.dsl.NativeCacheKind
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.utils.NativeCompilerDownloader
import org.jetbrains.kotlin.konan.CompilerVersion
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.util.DependencyDirectories
import java.util.*

private val Project.jvmArgs
    get() = PropertiesProvider(this).nativeJvmArgs?.split("\\s+".toRegex()).orEmpty()

internal val Project.konanHome: String
    get() = PropertiesProvider(this).nativeHome?.let { file(it).absolutePath }
        ?: NativeCompilerDownloader(project).compilerDirectory.absolutePath

internal val Project.disableKonanDaemon: Boolean
    get() = PropertiesProvider(this).nativeDisableCompilerDaemon == true

internal val Project.konanVersion: CompilerVersion
    get() = PropertiesProvider(this).nativeVersion?.let { CompilerVersion.fromString(it) }
        ?: NativeCompilerDownloader.DEFAULT_KONAN_VERSION

internal val Project.konanCacheKind: NativeCacheKind
    get() = PropertiesProvider(this).nativeCacheKind

internal abstract class KotlinNativeToolRunner(
    protected val toolName: String,
    project: Project
) : KotlinToolRunner(project) {
    final override val displayName get() = toolName

    final override val mainClass get() = "org.jetbrains.kotlin.cli.utilities.MainKt"
    final override val daemonEntryPoint get() = "daemonMain"

    // We need to unset some environment variables which are set by XCode and may potentially affect the tool executed.
    override val environment = mapOf("LIBCLANG_DISABLE_CRASH_RECOVERY" to "1")
    final override val environmentBlacklist: Set<String> by lazy {
        HashSet<String>().also { collector ->
            KotlinNativeToolRunner::class.java.getResourceAsStream("/env_blacklist")?.let { stream ->
                stream.reader().use { r -> r.forEachLine { collector.add(it) } }
            }
        }
    }

    final override val systemProperties by lazy {
        mapOf(
            "konan.home" to project.konanHome,
            MessageRenderer.PROPERTY_KEY to MessageRenderer.GRADLE_STYLE.name,
            "java.library.path" to "${project.konanHome}/konan/nativelib"
        )
    }

    final override val classpath by lazy {
        project.fileTree("${project.konanHome}/konan/lib/").apply { include("*.jar") }.toSet()
    }

    final override fun checkClasspath() =
        check(classpath.isNotEmpty()) {
            """
                Classpath of the tool is empty: $toolName
                Probably the '${PropertiesProvider.KOTLIN_NATIVE_HOME}' project property contains an incorrect path.
                Please change it to the compiler root directory and rerun the build.
            """.trimIndent()
        }

    final override val isolatedClassLoaderCacheKey get() = project.konanHome

    override fun transformArgs(args: List<String>) = listOf(toolName) + args

    final override fun getCustomJvmArgs() = project.jvmArgs

    final override fun runInProcess(args: List<String>) {
        withParallelExecutionGuard {
            super.runInProcess(args)
        }
    }

    // TODO: Remove once KT-37550 is fixed
    private inline fun withParallelExecutionGuard(action: () -> Unit) {
        try {
            if (PropertiesProvider(project).nativeEnableParallelExecutionCheck) {
                System.getProperties().compute(PARALLEL_EXECUTION_GUARD_PROPERTY) { _, value ->
                    check(value == null) { PARALLEL_EXECUTION_ERROR_MESSAGE }
                    "true"
                }
            }

            action()

        } finally {
            if (PropertiesProvider(project).nativeEnableParallelExecutionCheck) {
                System.clearProperty(PARALLEL_EXECUTION_GUARD_PROPERTY)
            }
        }
    }

    companion object {
        private const val PARALLEL_EXECUTION_GUARD_PROPERTY = "org.jetbrains.kotlin.native.compiler.running"

        private val PARALLEL_EXECUTION_ERROR_MESSAGE = """
            Parallel in-process execution of the Kotlin/Native compiler detected.
            
            At this moment the parallel execution of several compiler instances in the same process is not supported.
            To fix this, you can do one of the following things:
            
            - Disable in-process execution. To do this, set '${PropertiesProvider.KOTLIN_NATIVE_DISABLE_COMPILER_DAEMON}=true' project property.

            - Disable parallel task execution. To do this, set 'org.gradle.parallel=false' project property.
            
            If you still want to run the compiler in-process in parallel, you may disable this check by setting project
            property '${PropertiesProvider.KOTLIN_NATIVE_ENABLE_PARALLEL_EXECUTION_CHECK}=false'. Note that in this case the compiler may fail.
            
        """.trimIndent()
    }
}

/** A common ancestor for all runners that run the cinterop tool. */
internal abstract class AbstractKotlinNativeCInteropRunner(toolName: String, project: Project) : KotlinNativeToolRunner(toolName, project) {
    override val mustRunViaExec get() = true

    override val environment by lazy {
        val llvmExecutablesPath = llvmExecutablesPath
        if (llvmExecutablesPath != null)
            super.environment + ("PATH" to "$llvmExecutablesPath;${System.getenv("PATH")}")
        else
            super.environment
    }

    private val llvmExecutablesPath: String? by lazy {
        if (HostManager.host == KonanTarget.MINGW_X64) {
            // TODO: Read it from Platform properties when it is accessible.
            val konanProperties = Properties().apply {
                project.file("${project.konanHome}/konan/konan.properties").inputStream().use(::load)
            }

            konanProperties.getProperty("llvmHome.mingw_x64")?.let { toolchainDir ->
                DependencyDirectories.defaultDependenciesRoot
                    .resolve("$toolchainDir/bin")
                    .absolutePath
            }
        } else
            null
    }
}

/** Kotlin/Native C-interop tool runner */
internal class KotlinNativeCInteropRunner(project: Project) : AbstractKotlinNativeCInteropRunner("cinterop", project)

/** Kotlin/Native compiler runner */
internal class KotlinNativeCompilerRunner(project: Project) : KotlinNativeToolRunner("konanc", project) {
    private val useArgFile get() = project.disableKonanDaemon

    override val mustRunViaExec get() = project.disableKonanDaemon

    override fun transformArgs(args: List<String>): List<String> {
        if (!useArgFile) return super.transformArgs(args)

        val argFile = createTempFile(prefix = "kotlinc-native-args", suffix = ".lst").apply { deleteOnExit() }
        argFile.printWriter().use { w ->
            args.forEach { arg ->
                val escapedArg = arg
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                w.println("\"$escapedArg\"")
            }
        }

        return listOf(toolName, "@${argFile.absolutePath}")
    }
}

/** Klib management tool runner */
internal class KotlinNativeKlibRunner(project: Project) : KotlinNativeToolRunner("klib", project) {
    override val mustRunViaExec get() = project.disableKonanDaemon
}

/** Platform libraries generation tool. Runs the cinterop tool under the hood. */
internal class KotlinNativeLibraryGenerationRunner(project: Project) :
    AbstractKotlinNativeCInteropRunner("generatePlatformLibraries", project)
{
    // The library generator works for a long time so enabling C2 can improve performance.
    override val disableC2: Boolean = false
}