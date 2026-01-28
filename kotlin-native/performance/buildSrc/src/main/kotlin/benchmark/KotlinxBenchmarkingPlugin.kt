package org.jetbrains.kotlin.benchmark

import kotlinx.benchmark.gradle.NativeBenchmarkExec
import kotlinx.benchmark.gradle.NativeBenchmarkTarget
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.getting
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.project
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.registering
import org.jetbrains.kotlin.BenchmarkRepeatingType
import org.jetbrains.kotlin.ConvertJMHReportTask
import org.jetbrains.kotlin.attempts
import org.jetbrains.kotlin.baseOnly
import org.jetbrains.kotlin.buildType
import org.jetbrains.kotlin.filter
import org.jetbrains.kotlin.filterRegex
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.hostKotlinNativeTargetName
import org.jetbrains.kotlin.hostTarget
import org.jetbrains.kotlin.kotlin
import org.jetbrains.kotlin.kotlinxBenchmark
import org.jetbrains.kotlin.nativeWarmup
import javax.inject.Inject

private val EXTENSION_NAME = "kotlinxBenchmark"

open class KotlinxBenchmarkExtension @Inject constructor(private val project: Project) : BenchmarkExtension(project) {
    /**
     * When benchmark name component ends with this postfix, hide the component from benchmarks name in the final report
     *
     * This is used to preserve names of the existing benchmarks and be able to compare the reports with older builds.
     *
     * Example:
     * if the benchmark is named `my.packageHideName.main.BenchmarksHideName.bench` and this property is set to `HideName`,
     * the benchmark name in the final report will be `my.main.bench`
     */
    val hideNamePostfix: Property<String> = project.objects.property(String::class.java).convention("HideName")

    val runBenchmark
        get() = project.tasks.named("${hostKotlinNativeTargetName}Benchmark", NativeBenchmarkExec::class)

    val konanRun by project.tasks.registering(ConvertJMHReportTask::class)
}

/**
 * A plugin configuring a benchmark Kotlin/Native project.
 */
open class KotlinxBenchmarkingPlugin : BenchmarkingPlugin() {
    override val Project.benchmark: KotlinxBenchmarkExtension
        get() = extensions.getByName(EXTENSION_NAME) as KotlinxBenchmarkExtension

    override fun Project.createExtension() = extensions.create<KotlinxBenchmarkExtension>(EXTENSION_NAME, this)

    override fun Project.configureTasks() {
        pluginManager.apply("org.jetbrains.kotlinx.benchmark")
        kotlin.apply {
            sourceSets.commonMain.dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:${kotlinxBenchmark.version}")
                implementation(project(":benchmarksKotlinxAdapter"))
            }
        }
        kotlinxBenchmark.apply {
            targets.register(hostKotlinNativeTargetName) {
                this as NativeBenchmarkTarget
                buildType = project.buildType
            }

            configurations.named("main").configure {
                benchmark.repeatingType.finalizeValue()
                val repeatingType = benchmark.repeatingType.get()
                warmups = nativeWarmup
                iterations = attempts
                mode = "AverageTime"
                outputTimeUnit = "us"
                when (repeatingType) {
                    BenchmarkRepeatingType.EXTERNAL -> {
                        iterationTime = 1
                        iterationTimeUnit = "ns"
                    }
                    BenchmarkRepeatingType.INTERNAL -> {
                        iterationTime = 1
                        iterationTimeUnit = "s"
                    }
                }
                advanced("nativeGCAfterIteration", true)
                advanced("nativeFork", when (repeatingType) {
                    BenchmarkRepeatingType.EXTERNAL -> "perIteration"
                    BenchmarkRepeatingType.INTERNAL -> "perBenchmark"
                })
                project.filter?.split(",")?.forEach {
                    include("\\b${it.replace(".", "\\.")}\\b")
                }
                project.filterRegex?.split(",")?.forEach {
                    include(it)
                }
                param("baseOnly", project.baseOnly)
            }
        }
        benchmark.konanRun.configure {
            group = BENCHMARKING_GROUP
            description = "Convert from kotlinx-benchmark report to the benchmarking report for Kotlin/Native."

            convertJMHReportClasspath.from(configurations.detachedConfiguration(dependencies.project(":benchmarksReports")))
            inputFile.fileProvider(benchmark.runBenchmark.map { it.reportFile })
            outputFile.set(layout.buildDirectory.file("nativeBenchResults.json"))
            if (benchmark.prefixBenchmarksWithApplicationName.get()) {
                arguments.add("-p")
                arguments.add(benchmark.applicationName.map { "$it::" })
            }
            arguments.add(benchmark.hideNamePostfix.map { "--hidePostfix=$it" })

            finalizedBy(benchmark.konanJsonReport)
        }
        benchmark.getCodeSize.configure {
            val linkTaskProvider = kotlin.hostTarget().binaries.getExecutable("${hostKotlinNativeTargetName}Benchmark", project.buildType).linkTaskProvider

            codeSizeBinary.fileProvider(linkTaskProvider.map { it.outputFile.get() })
        }
        benchmark.konanJsonReport.configure {
            val linkTaskProvider = kotlin.hostTarget().binaries.getExecutable("${hostKotlinNativeTargetName}Benchmark", project.buildType).linkTaskProvider

            benchmarksReports.from(benchmark.konanRun.map { it.outputFile.get() })
            compilerFlags.addAll(linkTaskProvider.map { it.toolOptions.freeCompilerArgs.get() })
        }
        // For some reason, the generated `*Benchmark` compilations do not inherit a dependency on cinterops from
        // the main compilations (even though they `associateWith` them). Just create a new cinterop in the new
        // compilation and copy the important configuration bits over.
        afterEvaluate {
            kotlin.apply {
                targets.filterIsInstance<KotlinNativeTarget>().forEach {
                    val main by it.compilations.getting

                    it.compilations.findByName("${it.name}Benchmark")?.apply {
                        cinterops {
                            main.cinterops.forEach {
                                create(it.name) {
                                    this.headers = it.headers
                                    this.extraOpts = it.extraOpts
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
