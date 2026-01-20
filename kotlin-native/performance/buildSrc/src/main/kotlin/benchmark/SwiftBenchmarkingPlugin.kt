package org.jetbrains.kotlin.benchmark

import org.gradle.api.Project
import org.gradle.kotlin.dsl.project
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.*
import java.io.File
import javax.inject.Inject
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import java.nio.file.Path
import kotlin.reflect.KClass

open class SwiftBenchmarkExtension @Inject constructor(project: Project) : BenchmarkExtension(project) {
    var swiftSources: List<String> = emptyList()
}

/**
 * A plugin configuring a benchmark Kotlin/Native project.
 */
open class SwiftBenchmarkingPlugin : BenchmarkingPlugin() {
    override val benchmarkExtensionClass: KClass<*>
        get() = SwiftBenchmarkExtension::class

    override val Project.benchmark: SwiftBenchmarkExtension
        get() = extensions.getByName(benchmarkExtensionName) as SwiftBenchmarkExtension

    override val benchmarkExtensionName: String = "swiftBenchmark"

    override val Project.nativeLinkBinary: String
        get() = File("${framework.outputFile.absolutePath}/$nativeFrameworkName").canonicalPath

    override val Project.nativeLinkTaskArguments: List<String>
        get() = framework.freeCompilerArgs.map { "\"$it\"" }

    private lateinit var framework: Framework
    val nativeFrameworkName = "benchmark"

    override fun KotlinNativeTarget.createNativeBinary(project: Project) {
        binaries.framework(nativeFrameworkName, listOf(project.benchmark.buildType)) {
            export(project.dependencies.project(":benchmarksLauncher"))
            // Specify settings configured by a user in the benchmark extension.
            project.afterEvaluate {
                linkerOpts.addAll(project.benchmark.linkerOpts)
            }
        }
    }

    override fun Project.createExtraTasks() {
        val nativeTarget = hostKotlinNativeTarget
        // Build executable from swift code.
        framework = nativeTarget.binaries.getFramework(nativeFrameworkName, benchmark.buildType)
        tasks.create("buildSwift") {
            dependsOn(framework.linkTaskName)
            doLast {
                val frameworkParentDirPath = framework.outputDirectory.absolutePath
                val options = listOf("-O", "-wmo", "-Xlinker", "-rpath", "-Xlinker", frameworkParentDirPath, "-F", frameworkParentDirPath)
                compileSwift(project, nativeTarget.konanTarget, benchmark.swiftSources, options,
                        Paths.get(layout.buildDirectory.get().asFile.absolutePath, benchmark.applicationName))
            }
        }
    }

    override fun KotlinMultiplatformExtension.configureTargets() {
        macosArm64()
    }

    override fun RunKotlinNativeTask.configureKonanRunTask() {
        executable.set(project.layout.buildDirectory.file(project.benchmark.applicationName))
        dependsOn("buildSwift")
    }

    fun Array<String>.runCommand(
            workingDir: File = File("."),
            timeoutAmount: Long = 60,
            timeoutUnit: TimeUnit = TimeUnit.SECONDS,
            env: Map<String, String>,
    ): String {
        return try {
            val processBuilder = ProcessBuilder(*this)
                    .directory(workingDir)
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.PIPE)
            env.forEach { key, value ->
                processBuilder.environment().set(key, value)
            }
            val process = processBuilder.start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor(timeoutAmount, timeoutUnit)
            output
        } catch (e: Exception) {
            println("Couldn't run command ${this.joinToString(" ")}")
            println(e.stackTrace.joinToString("\n"))
            error(e.message!!)
        }
    }
    fun compileSwift(
            project: Project, target: KonanTarget, sources: List<String>, options: List<String>,
            output: Path
    ) {
        val platform = project.platformManager.platform(target)
        assert(platform.configurables is AppleConfigurables)
        val configs = platform.configurables as AppleConfigurables
        val compiler = configs.absoluteTargetToolchain + "/bin/swiftc"

        val swiftTarget = configs.targetTriple.withOSVersion(configs.osVersionMin).toString()

        val args = listOf("-sdk", configs.absoluteTargetSysRoot, "-target", swiftTarget) +
                options + "-o" + output.toString() + sources

        val out = mutableListOf<String>().apply {
            add(compiler)
            addAll(args)
        }.toTypedArray().runCommand(
            timeoutAmount = 240,
            env = mapOf("DYLD_FALLBACK_FRAMEWORK_PATH" to File(configs.absoluteTargetToolchain).parent + "/ExtraFrameworks"),
        )

        println(
                """
        |$compiler finished with:
        |options: ${args.joinToString(separator = " ")}
        |output: $out
        """.trimMargin()
        )
        check(output.toFile().exists()) { "Compiler swiftc hasn't produced an output file: $output" }
    }
}
