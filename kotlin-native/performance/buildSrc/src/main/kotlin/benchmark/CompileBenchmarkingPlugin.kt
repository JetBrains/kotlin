package org.jetbrains.kotlin.benchmark

import org.gradle.api.*
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.TaskState
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.*
import org.jetbrains.report.BenchmarkResult
import javax.inject.Inject

private fun getCompileBenchmarkTime(subproject: Project,
                            programName: String, tasksNames: Iterable<String>,
                            repeats: Int, exitCodes: Map<String, Int>) =
        (1..repeats).map { number ->
            var time = 0.0
            var status = BenchmarkResult.Status.PASSED
            tasksNames.forEach {
                time += TaskTimerListener.getTimerListenerOfSubproject(subproject).getTime("$it$number")
                status = if (exitCodes["$it$number"] != 0) BenchmarkResult.Status.FAILED else status
            }

            BenchmarkResult(programName, status, time, BenchmarkResult.Metric.COMPILE_TIME, time, number, 0)
        }.toList()

// Class time tracker for all tasks.
private class TaskTimerListener: TaskExecutionListener {
    companion object {
        internal val timerListeners = mutableMapOf<String, TaskTimerListener>()

        internal fun getTimerListenerOfSubproject(subproject: Project) =
                timerListeners[subproject.name] ?: error("TimeListener for project ${subproject.name} wasn't set")
    }

    val tasksTimes = mutableMapOf<String, Double>()

    fun getTime(taskName: String) = tasksTimes[taskName] ?: 0.0

    private var startTime = System.nanoTime()

    override fun beforeExecute(task: Task) {
        startTime = System.nanoTime()
    }

    override fun afterExecute(task: Task, taskState: TaskState) {
        tasksTimes[task.name] = (System.nanoTime() - startTime) / 1000.0
    }
}

private fun addTimeListener(subproject: Project) {
    val listener = TaskTimerListener()
    TaskTimerListener.timerListeners.put(subproject.name, listener)
    subproject.gradle.addListener(listener)
}

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
}

open class CompileBenchmarkExtension @Inject constructor(val project: Project) {
    var applicationName = project.name
    var repeatNumber: Int = 1
    var buildSteps: BuildStepContainer = BuildStepContainer(project)
    var compilerOpts: List<String> = emptyList()

    fun buildSteps(configure: Action<BuildStepContainer>): Unit = buildSteps.let { configure.execute(it) }
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

            group = BENCHMARKING_GROUP
            description = "Runs the compile only benchmark for Kotlin/Native."

            doLast {
                val nativeCompileTime = getCompileBenchmarkTime(
                        project,
                        this@with.applicationName,
                        buildSteps.names,
                        repeatNumber,
                        exitCodes
                )
                layout.buildDirectory.file(nativeBenchResults).get().asFile.writeText(
                        nativeCompileTime.joinToString(prefix = "[", postfix = "]") { it.toJson() }
                )
            }
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
        val konanJsonReport by tasks.registering(JsonReportTask::class) {
            group = BENCHMARKING_GROUP
            description = "Builds the benchmarking report for Kotlin/Native."

            this.applicationName.set(this@with.applicationName)
            codeSizeBinary.set(layout.buildDirectory.file("program${getNativeProgramExtension()}"))
            benchmarksReportFile.set(layout.buildDirectory.file(nativeBenchResults))
            dependsOn(konanRun)
            compilerVersion.set(project.konanVersion)
            compilerFlags.set(getCompilerFlags(benchmarkExtension).sorted())
            kotlinVersion.set(project.kotlinVersion)
            reportFile.set(layout.buildDirectory.file(nativeJson))
        }
        konanRun.finalizedBy(konanJsonReport)
    }

    private fun getCompilerFlags(benchmarkExtension: CompileBenchmarkExtension) =
            benchmarkExtension.compilerOpts
    
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
    }

    companion object {
        const val COMPILE_BENCHMARK_EXTENSION_NAME = "compileBenchmark"
    }
}
