package org.jetbrains.kotlin.benchmark

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Usage
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.project
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.registering
import org.jetbrains.kotlin.*
import javax.inject.Inject

internal const val BENCHMARKING_GROUP = "benchmarking"
internal const val BENCHMARK_SEMAPHORE_NAME = "benchmarkSemaphore"

abstract class BenchmarkSemaphore : BuildService<BuildServiceParameters.None>

open class BenchmarkExtension @Inject constructor(project: Project) {
    /**
     * Benchmarks group name.
     *
     * - each benchmark will be prefixed with `applicationName::` (unless [prefixBenchmarksWithApplicationName] is set to `false`)
     * - code size metric (i.e. the size of the Kotlin-produced artifact for this benchmark group) will be named `applicationName`
     * - for [SwiftBenchmarkingPlugin] this is used as the "product name" to use in the corresponding `Package.swift`
     */
    val applicationName: Property<String> = project.objects.property(String::class.java).convention(project.name)

    /**
     * Additional compiler options to use for this benchmark group
     *
     * The benchmark will apply both this and contents of [compilerArgs] Gradle property
     */
    val compilerOpts: ListProperty<String> = project.objects.listProperty(String::class.java)

    /**
     * Whether this benchmark should perform warmup and repetitions itself or be externally driven.
     */
    val repeatingType: Property<BenchmarkRepeatingType> = project.objects.property(BenchmarkRepeatingType::class.java).convention(BenchmarkRepeatingType.INTERNAL)

    /**
     * Whether to prepend [applicationName::][applicationName] to each benchmark run.
     */
    val prefixBenchmarksWithApplicationName: Property<Boolean> = project.objects.property(Boolean::class.java).convention(true)

    val konanRun by project.tasks.registering(RunKotlinNativeTask::class)
    val getCodeSize by project.tasks.registering(CodeSizeTask::class)
    val konanJsonReport by project.tasks.registering(JsonReportTask::class)

    val benchmarkSemaphore = project.gradle.sharedServices.registerIfAbsent(BENCHMARK_SEMAPHORE_NAME, BenchmarkSemaphore::class.java) {
        maxParallelUsages.set(1) // Benchmarks should not be executed in parallel to each other: this will skew their results
    }
}

/**
 * A plugin configuring a benchmark Kotlin/Native project.
 */
abstract class BenchmarkingPlugin : Plugin<Project> {
    protected abstract val Project.benchmark: BenchmarkExtension
    protected abstract fun Project.createExtension(): BenchmarkExtension

    protected abstract fun Project.configureTasks()

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
        }

        benchmark.konanRun.configure {
            group = BENCHMARKING_GROUP
            description = "Runs the benchmark for Kotlin/Native."

            reportFile.set(layout.buildDirectory.file("nativeBenchResults.json"))
            verbose.convention(logger.isInfoEnabled)
            baseOnly.convention(project.baseOnly)
            filter.convention(project.filter)
            filterRegex.convention(project.filterRegex)
            warmupCount.convention(nativeWarmup)
            repeatCount.convention(attempts)
            repeatingType.set(benchmark.repeatingType)
            if (benchmark.prefixBenchmarksWithApplicationName.get()) {
                arguments.add("-p")
                arguments.add(benchmark.applicationName.map { "$it::" })
            }
            useCSet.convention(project.useCSet)

            usesService(benchmark.benchmarkSemaphore)

            // We do not want to cache benchmarking runs; we want the task to run whenever requested.
            outputs.upToDateWhen { false }

            // TODO: Gradle fails with
            //       Unable to make progress running work. The following items are queued for execution but none of them can be started
            //       When calling something like ./gradlew :helloworld :logging
            //       Removing this finalizedBy somehow helps
//            finalizedBy(benchmark.konanJsonReport)
        }

        benchmark.getCodeSize.configure {
            group = BENCHMARKING_GROUP
            description = "Collect the code size metric for Kotlin/Native."

            name.set(benchmark.applicationName)
            reportFile.set(layout.buildDirectory.file("nativeCodeSizeResults.json"))
        }

        benchmark.konanJsonReport.configure {
            group = BENCHMARKING_GROUP
            description = "Builds the benchmarking report for Kotlin/Native."

            benchmarksReports.from(benchmark.getCodeSize)
            benchmarksReports.from(benchmark.konanRun.map { it.reportFile.get() })
            compilerVersion.set(project.compilerVersion)
            if (buildType.optimized) {
                compilerFlags.add("-opt")
            }
            if (buildType.debuggable) {
                compilerFlags.add("-g")
            }
            reportFile.set(layout.buildDirectory.file(nativeJson))
        }

        val nativeReportElements by configurations.creating {
            isCanBeConsumed = true
            isCanBeResolved = false
            attributes {
                attribute(Usage.USAGE_ATTRIBUTE, objects.named("native-report"))
            }
        }

        artifacts {
            add(nativeReportElements.name, benchmark.konanJsonReport)
        }

        configureTasks()
    }
}
