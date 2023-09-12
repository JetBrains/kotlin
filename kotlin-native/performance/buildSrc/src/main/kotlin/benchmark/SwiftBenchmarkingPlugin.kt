package org.jetbrains.kotlin.benchmark

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.Task
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinNativeTargetPreset
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.konan.target.*
import java.io.File
import javax.inject.Inject
import java.nio.file.Paths
import java.net.URL
import java.util.concurrent.TimeUnit
import java.nio.file.Path
import kotlin.reflect.KClass

enum class CodeSizeEntity { FRAMEWORK, EXECUTABLE }

open class SwiftBenchmarkExtension @Inject constructor(project: Project) : BenchmarkExtension(project) {
    var swiftSources: List<String> = emptyList()
    var useCodeSize: CodeSizeEntity = CodeSizeEntity.FRAMEWORK         // use as code size metric framework size or executable
}

/**
 * A plugin configuring a benchmark Kotlin/Native project.
 */
open class SwiftBenchmarkingPlugin : BenchmarkingPlugin() {
    override fun Project.configureJvmJsonTask(jvmRun: Task): Task {
        return tasks.create("jvmJsonReport") {
            logger.info("JVM run is unsupported")
            jvmRun.finalizedBy(this)
        }
    }

    override fun Project.configureJvmTask(): Task {
        return tasks.create("jvmRun") {
            doLast {
                logger.info("JVM run is unsupported")
            }
        }
    }

    override val benchmarkExtensionClass: KClass<*>
        get() = SwiftBenchmarkExtension::class

    override val Project.benchmark: SwiftBenchmarkExtension
        get() = extensions.getByName(benchmarkExtensionName) as SwiftBenchmarkExtension

    override val benchmarkExtensionName: String = "swiftBenchmark"

    override val Project.nativeExecutable: String
        get() = Paths.get(buildDir.absolutePath, benchmark.applicationName).toString()

    override val Project.nativeLinkTask: Task
        get() = tasks.getByName("buildSwift")

    private lateinit var framework: Framework
    val nativeFrameworkName = "benchmark"

    override fun NamedDomainObjectContainer<KotlinSourceSet>.configureSources(project: Project) {
        project.benchmark.let {
            commonMain.kotlin.srcDirs(*it.commonSrcDirs.toTypedArray())
            nativeMain.kotlin.srcDirs(*(it.nativeSrcDirs).toTypedArray())
        }
    }

    override fun Project.determinePreset(): AbstractKotlinNativeTargetPreset<*> =
            defaultHostPreset(this).also { preset ->
                logger.quiet("$project has been configured for ${preset.name} platform.")
            } as AbstractKotlinNativeTargetPreset<*>

    override fun KotlinNativeTarget.configureNativeOutput(project: Project) {
        binaries.framework(nativeFrameworkName, listOf(project.benchmark.buildType)) {
            // Specify settings configured by a user in the benchmark extension.
            project.afterEvaluate {
                linkerOpts.addAll(project.benchmark.linkerOpts)
            }
        }
    }

    override fun Project.configureExtraTasks() {
        val nativeTarget = kotlin.targets.getByName(NATIVE_TARGET_NAME) as KotlinNativeTarget
        // Build executable from swift code.
        framework = nativeTarget.binaries.getFramework(nativeFrameworkName, benchmark.buildType)
        tasks.create("buildSwift") {
            dependsOn(framework.linkTaskName)
            doLast {
                val frameworkParentDirPath = framework.outputDirectory.absolutePath
                val options = listOf("-O", "-wmo", "-Xlinker", "-rpath", "-Xlinker", frameworkParentDirPath, "-F", frameworkParentDirPath)
                compileSwift(project, nativeTarget.konanTarget, benchmark.swiftSources, options,
                        Paths.get(buildDir.absolutePath, benchmark.applicationName), false)
            }
        }
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
                    .redirectOutput(ProcessBuilder.Redirect.PIPE)
                    .redirectError(ProcessBuilder.Redirect.PIPE)
            env.forEach { key, value ->
                processBuilder.environment().set(key, value)
            }
            processBuilder.start().apply {
                waitFor(timeoutAmount, timeoutUnit)
            }.inputStream.bufferedReader().readText()
        } catch (e: Exception) {
            println("Couldn't run command ${this.joinToString(" ")}")
            println(e.stackTrace.joinToString("\n"))
            error(e.message!!)
        }
    }
    fun compileSwift(
            project: Project, target: KonanTarget, sources: List<String>, options: List<String>,
            output: Path, fullBitcode: Boolean = false
    ) {
        val platform = project.platformManager.platform(target)
        assert(platform.configurables is AppleConfigurables)
        val configs = platform.configurables as AppleConfigurables
        val compiler = configs.absoluteTargetToolchain + "/usr/bin/swiftc"

        val swiftTarget = configs.targetTriple.withOSVersion(configs.osVersionMin).toString()

        val args = listOf("-sdk", configs.absoluteTargetSysRoot, "-target", swiftTarget) +
                options + "-o" + output.toString() + sources +
                if (fullBitcode) listOf("-embed-bitcode", "-Xlinker", "-bitcode_verify") else listOf("-embed-bitcode-marker")

        val out = mutableListOf<String>().apply {
            add(compiler)
            addAll(args)
        }.toTypedArray().runCommand(
            timeoutAmount = 240,
            env = mapOf("DYLD_FALLBACK_FRAMEWORK_PATH" to configs.absoluteTargetToolchain + "/ExtraFrameworks"),
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

    override fun Project.collectCodeSize(applicationName: String) =
            getCodeSizeBenchmark(applicationName,
                    if (benchmark.useCodeSize == CodeSizeEntity.FRAMEWORK)
                        File("${framework.outputFile.absolutePath}/$nativeFrameworkName").canonicalPath
                    else
                        nativeExecutable
            )

    override fun getCompilerFlags(project: Project, nativeTarget: KotlinNativeTarget) =
            if (project.benchmark.useCodeSize == CodeSizeEntity.FRAMEWORK) {
                super.getCompilerFlags(project, nativeTarget) + framework.freeCompilerArgs.map { "\"$it\"" }
            } else {
                listOf("-O", "-wmo")
            }
}
