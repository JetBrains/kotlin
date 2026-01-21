package org.jetbrains.kotlin.benchmark

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.project
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import javax.inject.Inject
import kotlin.collections.plus
import kotlin.reflect.KClass

internal val Project.nativeWarmup: Int
    get() = (property("nativeWarmup") as String).toInt()

internal val Project.attempts: Int
    get() = (property("attempts") as String).toInt()

internal val Project.nativeBenchResults: String
    get() = property("nativeBenchResults") as String

// Gradle property to add flags to benchmarks run from command line.
internal val Project.compilerArgs: List<String>
    get() = (findProperty("compilerArgs") as String?)?.split("\\s".toRegex()).orEmpty()

internal val Project.kotlinVersion: String
    get() = property("kotlinVersion") as String

internal val Project.konanVersion: String?
    get() = findProperty("konanVersion") as String?

internal val Project.nativeJson: String
    get() = project.property("nativeJson") as String

internal val Project.buildType: NativeBuildType
    get() = (findProperty("nativeBuildType") as String?)?.let { NativeBuildType.valueOf(it) } ?: NativeBuildType.RELEASE

internal val Project.useCSet: Boolean
    get() = (findProperty("useCSet") as String?).toBoolean()

open class BenchmarkExtension @Inject constructor(val project: Project) {
    var applicationName: String = project.name
    var compileTasks: List<String> = emptyList()
    var linkerOpts: Collection<String> = emptyList()
    var compilerOpts: List<String> = emptyList()
    var buildType: NativeBuildType = project.buildType
    var repeatingType: BenchmarkRepeatingType = BenchmarkRepeatingType.INTERNAL
}

/**
 * A plugin configuring a benchmark Kotlin/Native project.
 */
abstract class BenchmarkingPlugin: Plugin<Project> {
    protected abstract val Project.benchmark: BenchmarkExtension
    protected abstract val benchmarkExtensionName: String
    protected abstract val benchmarkExtensionClass: KClass<*>

    protected open fun KotlinNativeTarget.createNativeBinary(project: Project) {
        binaries.executable(NATIVE_EXECUTABLE_NAME, listOf(project.benchmark.buildType)) {
            this.runTaskProvider?.configure {
                group = ""
                enabled = false
            }
        }
    }

    protected open fun KotlinMultiplatformExtension.configureTargets() {
        benchmarkingTargets()
    }

    private fun Project.configureKotlinProject() {
        kotlin.apply {
            configureTargets()
            sourceSets.commonMain.dependencies {
                // All benchmarks require a benchmarks launcher.
                // swiftinterop benchmarks also have to export it via ObjCExport => api instead of implementation dependency
                api(project.dependencies.project(":benchmarksLauncher"))
            }
            compilerOptions {
                freeCompilerArgs.addAll(benchmark.compilerOpts + compilerArgs)
            }
            targets.filterIsInstance<KotlinNativeTarget>().forEach {
                it.createNativeBinary(project)
            }
        }
    }

    protected abstract fun RunKotlinNativeTask.configureKonanRunTask()

    private fun Project.createKonanRunTask() {
        tasks.register<RunKotlinNativeTask>("konanRun") {
            group = BENCHMARKING_GROUP
            description = "Runs the benchmark for Kotlin/Native."

            reportFile.set(layout.buildDirectory.file(nativeBenchResults))
            verbose.convention(logger.isInfoEnabled)
            baseOnly.convention(false)
            warmupCount.convention(nativeWarmup)
            repeatCount.convention(attempts)
            repeatingType.set(benchmark.repeatingType)
            arguments.addAll("-p", "${benchmark.applicationName}::")
            useCSet.convention(project.useCSet)

            // We do not want to cache benchmarking runs; we want the task to run whenever requested.
            outputs.upToDateWhen { false }

            finalizedBy("konanJsonReport")

            configureKonanRunTask()
        }
    }

    protected abstract fun JsonReportTask.configureKonanJsonReportTask()

    private fun Project.createKonanJsonReportTask() {
        tasks.register<JsonReportTask>("konanJsonReport") {
            group = BENCHMARKING_GROUP
            description = "Builds the benchmarking report for Kotlin/Native."

            applicationName.set(benchmark.applicationName)
            benchmarksReportFile.set(layout.buildDirectory.file(nativeBenchResults))
            compilerVersion.set(project.konanVersion)
            if (benchmark.buildType.optimized) {
                compilerFlags.add("-opt")
            }
            if (benchmark.buildType.debuggable) {
                compilerFlags.add("-g")
            }
            kotlinVersion.set(project.kotlinVersion)
            reportFile.set(layout.buildDirectory.file(nativeJson))

            configureKonanJsonReportTask()
        }
    }

    protected open fun Project.createExtraTasks() {}

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("kotlin-multiplatform")

        // Use Kotlin compiler version specified by the project property.
        target.logger.info("BenchmarkingPlugin.kt:apply($kotlinVersion)")

        extensions.create(benchmarkExtensionName, benchmarkExtensionClass.java, this)
        configureKotlinProject()
        createExtraTasks()
        createKonanRunTask()
        createKonanJsonReportTask()
    }

    companion object {
        const val NATIVE_EXECUTABLE_NAME = "benchmark"
        const val BENCHMARKING_GROUP = "benchmarking"
    }
}
