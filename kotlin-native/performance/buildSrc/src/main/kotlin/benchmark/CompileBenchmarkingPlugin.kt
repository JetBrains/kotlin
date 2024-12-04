package org.jetbrains.kotlin.benchmark

import groovy.lang.Closure
import org.gradle.api.*
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.Exec
import org.jetbrains.kotlin.*
import javax.inject.Inject

class BuildStep (private val _name: String): Named  {
    override fun getName(): String = _name
    lateinit var command: List<String>

    fun command(vararg command: String) {
        this.command = command.toList()
    }
}

class BuildStepContainer(val project: Project): NamedDomainObjectContainer<BuildStep> by project.container(BuildStep::class.java) {
    fun step(name: String, configure: Action<BuildStep>) =
        maybeCreate(name).apply { configure.execute(this) }
    
    fun step(name: String, configure: Closure<Unit>) =
        step(name, { project.configure(this, configure) })
}

open class CompileBenchmarkExtension @Inject constructor(val project: Project) {
    var applicationName = project.name
    var repeatNumber: Int = 1
    var buildSteps: BuildStepContainer = BuildStepContainer(project)
    var compilerOpts: List<String> = emptyList()

    fun buildSteps(configure: Action<BuildStepContainer>): Unit = buildSteps.let { configure.execute(it) }
    fun buildSteps(configure: Closure<Unit>): Unit = buildSteps { project.configure(this, configure) }
}

open class CompileBenchmarkingPlugin : Plugin<Project> {

    private val exitCodes: MutableMap<String, Int> = mutableMapOf()
    
    private fun Project.configureUtilityTasks() {
        tasks.create("configureBuild") {
            doLast { mkdir(layout.buildDirectory.get().asFile) }
        }

        tasks.create("clean", Delete::class.java) {
            delete(layout.buildDirectory)
        }
    }
    
    private fun Project.configureKonanRun(
        benchmarkExtension: CompileBenchmarkExtension
    ): Unit = with(benchmarkExtension) {
        // Aggregate task.
        val konanRun = tasks.create("konanRun") {
            dependsOn("configureBuild")

            group = BenchmarkingPlugin.BENCHMARKING_GROUP
            description = "Runs the compile only benchmark for Kotlin/Native."
        }

        // Compile tasks.
        afterEvaluate {
            for (number in 1..repeatNumber) {
                buildSteps.forEach { step ->
                    val taskName = step.name
                    tasks.create("$taskName$number", Exec::class.java).apply {
                        commandLine(step.command)
                        isIgnoreExitValue = true
                        konanRun.dependsOn(this)
                        doLast {
                            exitCodes[name] = executionResult.get().exitValue
                        }
                    }
                }
            }
        }

        // Report task.
        tasks.create("konanJsonReport").apply {

            group = BenchmarkingPlugin.BENCHMARKING_GROUP
            description = "Builds the benchmarking report for Kotlin/Native."

            doLast {
                val nativeCompileTime = getCompileBenchmarkTime(
                    project,
                    applicationName,
                    buildSteps.names,
                    repeatNumber,
                    exitCodes
                )
                val nativeExecutable = layout.buildDirectory.file("program${getNativeProgramExtension()}").get().asFile
                val properties = commonBenchmarkProperties + mapOf(
                    "type" to "native",
                    "compilerVersion" to konanVersion,
                    "benchmarks" to "[]",
                    "flags" to getCompilerFlags(benchmarkExtension).sorted(),
                    "compileTime" to nativeCompileTime,
                    "codeSize" to getCodeSizeBenchmark(applicationName, nativeExecutable.absolutePath)
                )
                val output = createJsonReport(properties)
                layout.buildDirectory.file(nativeJson).get().asFile.writeText(output)
            }
            konanRun.finalizedBy(this)
        }
    }

    private fun getCompilerFlags(benchmarkExtension: CompileBenchmarkExtension) =
            benchmarkExtension.compilerOpts

    private fun Project.configureJvmRun() {
        val jvmRun = tasks.create("jvmRun") {
            group = BenchmarkingPlugin.BENCHMARKING_GROUP
            description = "Runs the compile only benchmark for Kotlin/JVM."
            doLast { println("JVM run isn't supported") }
        }

        tasks.create("jvmJsonReport") {
            group = BenchmarkingPlugin.BENCHMARKING_GROUP
            description = "Builds the benchmarking report for Kotlin/Native."
            doLast { println("JVM run isn't supported") }
            jvmRun.finalizedBy(this)
        }
    }
    
    override fun apply(target: Project): Unit = with(target) {
        addTimeListener(this)

        val benchmarkExtension = extensions.create(
            COMPILE_BENCHMARK_EXTENSION_NAME,
            CompileBenchmarkExtension::class.java,
            this
        )

        // Create tasks.
        configureUtilityTasks()
        configureKonanRun(benchmarkExtension)
        configureJvmRun()
    }

    companion object {
        const val COMPILE_BENCHMARK_EXTENSION_NAME = "compileBenchmark"
    }
}
