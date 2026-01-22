package org.jetbrains.kotlin.benchmark

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import javax.inject.Inject

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

val Project.buildType: NativeBuildType
    get() = (findProperty("nativeBuildType") as String?)?.let { NativeBuildType.valueOf(it) } ?: NativeBuildType.RELEASE

internal val Project.useCSet: Boolean
    get() = (findProperty("useCSet") as String?).toBoolean()

internal const val BENCHMARKING_GROUP = "benchmarking"

open class BenchmarkExtension @Inject constructor(project: Project) {
    val applicationName: Property<String> = project.objects.property(String::class.java).convention(project.name)
    val compilerOpts: ListProperty<String> = project.objects.listProperty(String::class.java)
    val repeatingType: Property<BenchmarkRepeatingType> = project.objects.property(BenchmarkRepeatingType::class.java).convention(BenchmarkRepeatingType.INTERNAL)
    val prefixBenchmarksWithApplicationName: Property<Boolean> = project.objects.property(Boolean::class.java).convention(true)

    val konanRun by project.tasks.registering(RunKotlinNativeTask::class)
    val konanJsonReport by project.tasks.registering(JsonReportTask::class)
}

/**
 * A plugin configuring a benchmark Kotlin/Native project.
 */
abstract class BenchmarkingPlugin: Plugin<Project> {
    protected abstract val Project.benchmark: BenchmarkExtension
    protected abstract fun Project.createExtension(): BenchmarkExtension

    protected abstract fun Project.createNativeBinary(target: KotlinNativeTarget)
    protected abstract fun RunKotlinNativeTask.configureKonanRunTask()
    protected abstract fun JsonReportTask.configureKonanJsonReportTask()

    protected open fun Project.createExtraTasks() {}

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("kotlin-multiplatform")

        createExtension()

        kotlin.apply {
            sourceSets.commonMain.dependencies {
                // All benchmarks require a benchmarks launcher.
                // swiftinterop benchmarks also have to export it via ObjCExport => api instead of implementation dependency
                api(project.dependencies.project(":benchmarksLauncher"))
            }
            compilerOptions {
                freeCompilerArgs.addAll(benchmark.compilerOpts)
                freeCompilerArgs.addAll(compilerArgs)
            }
            targets.withType(KotlinNativeTarget::class).configureEach {
                createNativeBinary(this)
            }
        }

        createExtraTasks()

        benchmark.konanRun.configure {
            group = BENCHMARKING_GROUP
            description = "Runs the benchmark for Kotlin/Native."

            reportFile.set(layout.buildDirectory.file(nativeBenchResults))
            verbose.convention(logger.isInfoEnabled)
            baseOnly.convention(false)
            warmupCount.convention(nativeWarmup)
            repeatCount.convention(attempts)
            repeatingType.set(benchmark.repeatingType)
            if (benchmark.prefixBenchmarksWithApplicationName.get()) {
                arguments.add("-p")
                arguments.add(benchmark.applicationName.map { "$it::" })
            }
            useCSet.convention(project.useCSet)

            // We do not want to cache benchmarking runs; we want the task to run whenever requested.
            outputs.upToDateWhen { false }

            finalizedBy(benchmark.konanJsonReport)

            configureKonanRunTask()
        }

        benchmark.konanJsonReport.configure {
            group = BENCHMARKING_GROUP
            description = "Builds the benchmarking report for Kotlin/Native."

            applicationName.set(benchmark.applicationName)
            benchmarksReportFile.set(benchmark.konanRun.map { it.reportFile.get() })
            compilerVersion.set(project.konanVersion)
            if (buildType.optimized) {
                compilerFlags.add("-opt")
            }
            if (buildType.debuggable) {
                compilerFlags.add("-g")
            }
            kotlinVersion.set(project.kotlinVersion)
            reportFile.set(layout.buildDirectory.file(nativeJson))

            configureKonanJsonReportTask()
        }
    }
}
