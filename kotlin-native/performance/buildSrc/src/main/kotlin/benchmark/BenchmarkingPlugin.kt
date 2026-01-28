package org.jetbrains.kotlin.benchmark

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.project
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.registering
import org.jetbrains.kotlin.*
import javax.inject.Inject

internal const val BENCHMARKING_GROUP = "benchmarking"

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

    val getCodeSize by project.tasks.registering(CodeSizeTask::class)
    val konanJsonReport by project.tasks.registering(JsonReportTask::class)
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
            compilerOptions {
                freeCompilerArgs.addAll(benchmark.compilerOpts)
                freeCompilerArgs.addAll(compilerArgs)
            }
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
            compilerVersion.set(project.compilerVersion)
            if (buildType.optimized) {
                compilerFlags.add("-opt")
            }
            if (buildType.debuggable) {
                compilerFlags.add("-g")
            }
            reportFile.set(layout.buildDirectory.file(nativeJson))
        }

        configureTasks()
    }
}
