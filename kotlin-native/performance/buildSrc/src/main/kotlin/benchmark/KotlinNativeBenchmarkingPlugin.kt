package org.jetbrains.kotlin.benchmark

import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.project
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.registering
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.RunKotlinNativeTask
import org.jetbrains.kotlin.attempts
import org.jetbrains.kotlin.baseOnly
import org.jetbrains.kotlin.buildType
import org.jetbrains.kotlin.filter
import org.jetbrains.kotlin.filterRegex
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.hostTarget
import org.jetbrains.kotlin.kotlin
import org.jetbrains.kotlin.nativeWarmup
import org.jetbrains.kotlin.useCSet
import javax.inject.Inject

private const val NATIVE_EXECUTABLE_NAME = "benchmark"
private const val EXTENSION_NAME = "benchmark"

open class KotlinNativeBenchmarkExtension @Inject constructor(project: Project) : BenchmarkExtension(project) {
    val konanRun by project.tasks.registering(RunKotlinNativeTask::class)
}

/**
 * A plugin configuring a benchmark Kotlin/Native project.
 */
open class KotlinNativeBenchmarkingPlugin : BenchmarkingPlugin() {
    override val Project.benchmark: KotlinNativeBenchmarkExtension
        get() = extensions.getByName(EXTENSION_NAME) as KotlinNativeBenchmarkExtension

    override fun Project.createExtension() = extensions.create<KotlinNativeBenchmarkExtension>(EXTENSION_NAME, this)

    override fun Project.configureTasks() {
        kotlin.apply {
            sourceSets.commonMain.dependencies {
                // All benchmarks require a benchmarks launcher.
                implementation(project.dependencies.project(":benchmarksLauncher"))
            }
            targets.withType(KotlinNativeTarget::class).configureEach {
                binaries.executable(NATIVE_EXECUTABLE_NAME, listOf(project.buildType)) {
                    this.runTaskProvider?.configure {
                        group = ""
                        enabled = false
                    }
                }
            }
        }
        val linkTaskProvider = kotlin.hostTarget().binaries.getExecutable(NATIVE_EXECUTABLE_NAME, project.buildType).linkTaskProvider
        benchmark.konanRun.configure {
            group = BENCHMARKING_GROUP
            description = "Runs the benchmark for Kotlin/Native."

            executable.fileProvider(linkTaskProvider.map { it.outputFile.get() })
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

            // We do not want to cache benchmarking runs; we want the task to run whenever requested.
            outputs.upToDateWhen { false }

            finalizedBy(benchmark.konanJsonReport)
        }
        benchmark.getCodeSize.configure {
            codeSizeBinary.fileProvider(linkTaskProvider.map { it.outputFile.get() })
        }
        benchmark.konanJsonReport.configure {
            benchmarksReports.from(benchmark.konanRun.map { it.reportFile.get() })
            compilerFlags.addAll(linkTaskProvider.map { it.toolOptions.freeCompilerArgs.get() })
        }
    }
}
