package org.jetbrains.kotlin.benchmark

import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.hostKotlinNativeTarget
import org.jetbrains.kotlin.kotlin
import kotlin.apply

private const val NATIVE_EXECUTABLE_NAME = "benchmark"
private const val EXTENSION_NAME = "benchmark"

/**
 * A plugin configuring a benchmark Kotlin/Native project.
 */
open class KotlinNativeBenchmarkingPlugin: BenchmarkingPlugin() {
    override val Project.benchmark: BenchmarkExtension
        get() = extensions.getByName(EXTENSION_NAME) as BenchmarkExtension

    override fun Project.createExtension() = extensions.create<BenchmarkExtension>(EXTENSION_NAME, this)

    override fun Project.configureTasks() {
        kotlin.apply {
            targets.withType(KotlinNativeTarget::class).configureEach {
                binaries.executable(NATIVE_EXECUTABLE_NAME, listOf(project.buildType)) {
                    this.runTaskProvider?.configure {
                        group = ""
                        enabled = false
                    }
                }
            }
        }
        val linkTaskProvider = hostKotlinNativeTarget.binaries.getExecutable(NATIVE_EXECUTABLE_NAME, project.buildType).linkTaskProvider
        benchmark.konanRun.configure {
            executable.fileProvider(linkTaskProvider.map { it.outputFile.get() })
        }
        benchmark.konanJsonReport.configure {
            codeSizeBinary.fileProvider(linkTaskProvider.map { it.outputFile.get() })
            compilerFlags.addAll(linkTaskProvider.map { it.toolOptions.freeCompilerArgs.get() })
        }
    }
}
