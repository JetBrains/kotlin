package org.jetbrains.kotlin.benchmark

import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.project
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.*
import java.io.File
import javax.inject.Inject
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import java.nio.file.Path

private const val EXTENSION_NAME = "swiftBenchmark"

open class SwiftBenchmarkExtension @Inject constructor(project: Project) : BenchmarkExtension(project) {
    var swiftSources: List<String> = emptyList()
}

/**
 * A plugin configuring a benchmark Kotlin/Native project.
 */
open class SwiftBenchmarkingPlugin : BenchmarkingPlugin() {
    override val Project.benchmark: SwiftBenchmarkExtension
        get() = extensions.getByName(EXTENSION_NAME) as SwiftBenchmarkExtension

    override fun Project.createExtension() = extensions.create<SwiftBenchmarkExtension>(EXTENSION_NAME, this)

    private val Project.nativeLinkBinary: String
        get() = File("${framework.outputFile.absolutePath}/$nativeFrameworkName").canonicalPath

    private lateinit var framework: Framework
    val nativeFrameworkName = "benchmark"

    override fun Project.createNativeBinary(target: KotlinNativeTarget) {
        target.binaries.framework(nativeFrameworkName, listOf(project.buildType)) {
            export(dependencies.project(":benchmarksLauncher"))
        }
    }

    override fun Project.createExtraTasks() {
        val nativeTarget = hostKotlinNativeTarget
        // Build executable from swift code.
        framework = nativeTarget.binaries.getFramework(nativeFrameworkName, project.buildType)
        tasks.create("buildSwift") {
            dependsOn(framework.linkTaskName)
            doLast {
                val frameworkParentDirPath = framework.outputDirectory.absolutePath
                val options = listOf("-O", "-wmo", "-Xlinker", "-rpath", "-Xlinker", frameworkParentDirPath, "-F", frameworkParentDirPath)
                compileSwift(project, nativeTarget.konanTarget, benchmark.swiftSources, options,
                        Paths.get(layout.buildDirectory.get().asFile.absolutePath, benchmark.applicationName.get()))
            }
        }
    }

    override fun RunKotlinNativeTask.configureKonanRunTask() {
        executable.set(project.layout.buildDirectory.file(project.benchmark.applicationName.get()))
        dependsOn("buildSwift")
    }

    override fun JsonReportTask.configureKonanJsonReportTask() {
        codeSizeBinary.set(project.file("${framework.outputFile.absolutePath}/$nativeFrameworkName"))
        compilerFlags.addAll(framework.freeCompilerArgs.map { "\"$it\"" })
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
