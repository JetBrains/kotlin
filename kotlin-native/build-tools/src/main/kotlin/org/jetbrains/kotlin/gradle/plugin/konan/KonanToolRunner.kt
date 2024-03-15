/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.konan

import kotlinBuildProperties
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.konan.KonanPlugin.ProjectProperty.KONAN_HOME
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.nio.file.Files
import org.jetbrains.kotlin.konan.properties.resolvablePropertyString
import org.jetbrains.kotlin.konan.util.DependencyDirectories
import java.io.File
import java.util.Properties
import org.jetbrains.kotlin.compilerRunner.KotlinToolRunner
import org.jetbrains.kotlin.konan.target.AbstractToolConfig

internal interface KonanToolRunner {
    fun run(args: List<String>)
}

internal fun KonanToolRunner.run(vararg args: String) = run(args.toList())

private const val runFromDaemonPropertyName = "kotlin.native.tool.runFromDaemon"

@Suppress("DEPRECATION") // calling KotlinToolRunner(project) constructor is deprecated
internal abstract class KonanCliRunner(
        protected val toolName: String,
        project: Project,
        isolatedClassLoadersService: KonanCliRunnerIsolatedClassLoadersService,
        val additionalJvmArgs: List<String> = emptyList(),
        val konanHome: String = project.konanHome
) : KotlinToolRunner(project), KonanToolRunner {
    final override val displayName get() = toolName

    final override val mainClass get() = "org.jetbrains.kotlin.cli.utilities.MainKt"
    final override val daemonEntryPoint get() = "daemonMain"

    override val mustRunViaExec get() = false.also { System.setProperty(runFromDaemonPropertyName, "true") }

    final override val execSystemPropertiesBlacklist: Set<String>
        get() = super.execSystemPropertiesBlacklist + runFromDaemonPropertyName

    // We need to unset some environment variables which are set by XCode and may potentially affect the tool executed.
    final override val execEnvironmentBlacklist: Set<String> by lazy {
        HashSet<String>().also { collector ->
            KonanPlugin::class.java.getResourceAsStream("/env_blacklist")?.let { stream ->
                stream.reader().use { r -> r.forEachLine { collector.add(it) } }
            }
        }
    }

    final override val execSystemProperties by lazy { mapOf("konan.home" to konanHome) }

    final override val classpath by lazy {
        project.fileTree("$konanHome/konan/lib/").apply {
            include("trove4j.jar")
            include("kotlin-native-compiler-embeddable.jar")
        }.files
    }

    final override fun checkClasspath() =
            check(classpath.isNotEmpty()) {
                """
                Classpath of the tool is empty: $toolName
                Probably the '${KONAN_HOME.propertyName}' project property contains an incorrect path.
                Please change it to the compiler root directory and rerun the build.
            """.trimIndent()
            }

    data class IsolatedClassLoaderCacheKey(val classpath: Set<File>)

    // TODO: can't we use this for other implementations too?
    final override val isolatedClassLoaderCacheKey get() = IsolatedClassLoaderCacheKey(classpath)

    // A separate map for each build for automatic cleaning the daemon after the build have finished.
    final override val isolatedClassLoaders = isolatedClassLoadersService.isolatedClassLoaders

    override fun transformArgs(args: List<String>) = listOf(toolName) + args

    final override fun getCustomJvmArgs() = additionalJvmArgs
}

/** Kotlin/Native compiler runner */
internal class KonanCliCompilerRunner(
        project: Project,
        isolatedClassLoadersService: KonanCliRunnerIsolatedClassLoadersService,
        additionalJvmArgs: List<String> = emptyList(),
        val useArgFile: Boolean = true,
        konanHome: String = project.konanHome
) : KonanCliRunner("konanc", project, isolatedClassLoadersService, additionalJvmArgs, konanHome) {
    override fun transformArgs(args: List<String>): List<String> {
        if (!useArgFile) return super.transformArgs(args)

        val argFile = Files.createTempFile(/* prefix = */ "konancArgs", /* suffix = */ ".lst").toFile().apply { deleteOnExit() }
        argFile.printWriter().use { w ->
            for (arg in args) {
                val escapedArg = arg
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                w.println("\"$escapedArg\"")
            }
        }

        return listOf(toolName, "@${argFile.absolutePath}")
    }
}

private val load0 = Runtime::class.java.getDeclaredMethod("load0", Class::class.java, String::class.java).also {
    it.isAccessible = true
}

internal class CliToolConfig(konanHome: String, target: String) : AbstractToolConfig(konanHome, target, emptyMap()) {
    override fun loadLibclang() {
        // Load libclang into the system class loader. This is needed to allow developers to make changes
        // in the tooling infrastructure without having to stop the daemon (otherwise libclang might end up
        // loaded in two different class loaders which is not allowed by the JVM).
        load0.invoke(Runtime.getRuntime(), String::class.java, libclang)
    }
}

/** Kotlin/Native C-interop tool runner */
internal class KonanCliInteropRunner(
        private val project: Project,
        isolatedClassLoadersService: KonanCliRunnerIsolatedClassLoadersService,
        additionalJvmArgs: List<String> = emptyList(),
        konanHome: String = project.konanHome
) : KonanCliRunner("cinterop", project, isolatedClassLoadersService, additionalJvmArgs, konanHome) {
    private val projectDir = project.projectDir.toString()

    override val mustRunViaExec: Boolean
        get() = if (project.kotlinBuildProperties.getBoolean("kotlin.native.allowRunningCinteropInProcess")) {
            super.mustRunViaExec
        } else {
            true
        }

    override fun transformArgs(args: List<String>): List<String> {
        return super.transformArgs(args) + listOf("-Xproject-dir", projectDir)
    }

    override val execEnvironment by lazy {
        val result = mutableMapOf<String, String>()
        result.putAll(super.execEnvironment)
        result["LIBCLANG_DISABLE_CRASH_RECOVERY"] = "1"
        llvmExecutablesPath?.let {
            result["PATH"] = "$it;${System.getenv("PATH")}"
        }
        result
    }

    fun init(target: String) {
        CliToolConfig(konanHome, target).prepare()
    }

    private val llvmExecutablesPath: String? by lazy {
        if (HostManager.host == KonanTarget.MINGW_X64) {
            // TODO: Read it from Platform properties when it is accessible.
            val konanProperties = Properties().apply {
                project.file("$konanHome/konan/konan.properties").inputStream().use(::load)
            }

            konanProperties.resolvablePropertyString("llvmHome.mingw_x64")?.let { toolchainDir ->
                DependencyDirectories.defaultDependenciesRoot
                        .resolve("$toolchainDir/bin")
                        .absolutePath
            }
        } else
            null
    }
}
