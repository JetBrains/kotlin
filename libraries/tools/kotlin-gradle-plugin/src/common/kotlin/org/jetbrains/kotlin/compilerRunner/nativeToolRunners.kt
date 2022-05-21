/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner

import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.gradle.dsl.NativeCacheKind
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.isAtLeast
import org.jetbrains.kotlin.gradle.plugin.mpp.nativeUseEmbeddableCompilerJar
import org.jetbrains.kotlin.gradle.targets.native.KonanPropertiesBuildService
import org.jetbrains.kotlin.gradle.utils.NativeCompilerDownloader
import org.jetbrains.kotlin.konan.CompilerVersion
import org.jetbrains.kotlin.konan.properties.resolvablePropertyString
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.util.DependencyDirectories
import java.io.File
import java.nio.file.Files
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

internal fun Project.getKonanCacheKind(target: KonanTarget): NativeCacheKind {
    val commonCacheKind = PropertiesProvider(this).nativeCacheKind
    val targetCacheKind = PropertiesProvider(this).nativeCacheKindForTarget(target)
    return when {
        targetCacheKind != null -> targetCacheKind
        commonCacheKind != null -> commonCacheKind
        else -> KonanPropertiesBuildService.registerIfAbsent(gradle).get().defaultCacheKindForTarget(target)
    }
}

private val Project.kotlinNativeCompilerJar: String
    get() = if (nativeUseEmbeddableCompilerJar)
        "$konanHome/konan/lib/kotlin-native-compiler-embeddable.jar"
    else
        "$konanHome/konan/lib/kotlin-native.jar"

internal abstract class KotlinNativeToolRunner(
    protected val toolName: String,
    private val configuration: Configuration, // TODO: can be ambigious with Gradle Configuration
    executionContext: ExecutionContext
) : KotlinToolRunner(executionContext) {

    class Configuration(
        val konanVersion: CompilerVersion,
        val konanHome: String,
        val jvmArgs: List<String>,
        classpathProvider: () -> Set<File>,
    ) {
        val classpath by lazy(classpathProvider)

        constructor(project: Project) : this(
            konanVersion = project.konanVersion,
            konanHome = project.konanHome,
            jvmArgs = project.jvmArgs,
            classpathProvider = { //TODO: test this
                project.files(
                    project.kotlinNativeCompilerJar,
                    "${project.konanHome}/konan/lib/trove4j.jar"
                ).files
            }
        )
    }

    final override val displayName get() = toolName

    final override val mainClass get() = "org.jetbrains.kotlin.cli.utilities.MainKt"
    final override val daemonEntryPoint get() = "daemonMain"

    // We need to unset some environment variables which are set by XCode and may potentially affect the tool executed.
    final override val execEnvironmentBlacklist: Set<String> by lazy {
        HashSet<String>().also { collector ->
            KotlinNativeToolRunner::class.java.getResourceAsStream("/env_blacklist")?.let { stream ->
                stream.reader().use { r -> r.forEachLine { collector.add(it) } }
            }
        }
    }

    final override val execSystemProperties by lazy {
        // Still set konan.home for versions prior to 1.4-M3.
        val konanHomeRequired = configuration.konanVersion.let {
            !it.isAtLeast(1, 4, 0) ||
                    it.toString(showMeta = false, showBuild = false) in listOf("1.4-M1", "1.4-M2")
        }

        listOfNotNull(
            if (konanHomeRequired) "konan.home" to configuration.konanHome else null,
            MessageRenderer.PROPERTY_KEY to MessageRenderer.GRADLE_STYLE.name
        ).toMap()
    }

    final override val classpath by lazy { configuration.classpath }

    final override fun checkClasspath() =
        check(classpath.isNotEmpty()) {
            """
                Classpath of the tool is empty: $toolName
                Probably the '${PropertiesProvider.KOTLIN_NATIVE_HOME}' project property contains an incorrect path.
                Please change it to the compiler root directory and rerun the build.
            """.trimIndent()
        }

    data class IsolatedClassLoaderCacheKey(val classpath: Set<File>)

    // TODO: can't we use this for other implementations too?
    final override val isolatedClassLoaderCacheKey get() = IsolatedClassLoaderCacheKey(classpath)

    override fun transformArgs(args: List<String>) = listOf(toolName) + args

    final override fun getCustomJvmArgs() = configuration.jvmArgs
}

/** A common ancestor for all runners that run the cinterop tool. */
internal abstract class AbstractKotlinNativeCInteropRunner(
    toolName: String,
    project: Project
) : KotlinNativeToolRunner(toolName, Configuration(project), ExecutionContext.fromProject(project)) {
    override val mustRunViaExec get() = true

    override val execEnvironment by lazy {
        val result = mutableMapOf<String, String>()
        result.putAll(super.execEnvironment)
        result["LIBCLANG_DISABLE_CRASH_RECOVERY"] = "1"
        llvmExecutablesPath?.let {
            result["PATH"] = "$it;${System.getenv("PATH")}"
        }
        result
    }

    private val llvmExecutablesPath: String? by lazy {
        if (HostManager.host == KonanTarget.MINGW_X64) {
            // TODO: Read it from Platform properties when it is accessible.
            val konanProperties = Properties().apply {
                project.file("${project.konanHome}/konan/konan.properties").inputStream().use(::load)
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

/** Kotlin/Native C-interop tool runner */
internal class KotlinNativeCInteropRunner private constructor(private val project: Project) : AbstractKotlinNativeCInteropRunner("cinterop", project) {
    interface ExecutionContext {
        val project: Project
        fun runWithContext(action: () -> Unit)
    }

    override val defaultArguments: List<String>
        get() = mutableListOf<String>().apply {
            if (project.gradle.startParameter.isOffline) {
                addAll(listOf("-Xoverride-konan-properties", "airplaneMode=true"))
            }
        }

    companion object {
        fun ExecutionContext.run(args: List<String>) {
            val runner = KotlinNativeCInteropRunner(project)
            runWithContext { runner.run(args) }
        }
    }
}

/** Kotlin/Native compiler runner */
internal class KotlinNativeCompilerRunner(
    private val configuration: Configuration,
    executionContext: ExecutionContext
) : KotlinNativeToolRunner("konanc", configuration.parent, executionContext) {
    class Configuration(
        val parent: KotlinNativeToolRunner.Configuration,

        @get:Input
        val disableKonanDaemon: Boolean,

        @get:Input
        val isOffline: Boolean
    ) {
        constructor(project: Project) : this(
            parent = KotlinNativeToolRunner.Configuration(project),
            disableKonanDaemon = project.disableKonanDaemon,
            isOffline = project.gradle.startParameter.isOffline
        )
    }

    private val useArgFile get() = configuration.disableKonanDaemon

    override val mustRunViaExec get() = configuration.disableKonanDaemon

    override fun transformArgs(args: List<String>): List<String> {
        if (!useArgFile) return super.transformArgs(args)

        val argFile = Files.createTempFile(/* prefix = */ "kotlinc-native-args", /* suffix = */ ".lst").toFile().apply { deleteOnExit() }
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

    override val defaultArguments: List<String>
        get() = mutableListOf<String>().apply {
            if (configuration.isOffline) {
                add("-Xoverride-konan-properties=airplaneMode=true")
            }
        }
}

/** Platform libraries generation tool. Runs the cinterop tool under the hood. */
internal class KotlinNativeLibraryGenerationRunner(private val project: Project) :
    AbstractKotlinNativeCInteropRunner("generatePlatformLibraries", project) {
    // The library generator works for a long time so enabling C2 can improve performance.
    override val disableC2: Boolean = false

    override val defaultArguments: List<String>
        get() = mutableListOf<String>().apply {
            if (project.gradle.startParameter.isOffline) {
                addAll(listOf("-Xoverride-konan-properties", "airplaneMode=true"))
            }
        }
}
