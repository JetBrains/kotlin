package org.jetbrains.kotlin.benchmark

import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.create
import org.jetbrains.kotlin.JsonReportTask
import org.jetbrains.kotlin.RunKotlinNativeTask
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
import org.jetbrains.kotlin.hostKotlinNativeTarget

private const val NATIVE_EXECUTABLE_NAME = "benchmark"
private const val EXTENSION_NAME = "benchmark"

/**
 * A plugin configuring a benchmark Kotlin/Native project.
 */
open class KotlinNativeBenchmarkingPlugin: BenchmarkingPlugin() {
    override val Project.benchmark: BenchmarkExtension
        get() = extensions.getByName(EXTENSION_NAME) as BenchmarkExtension

    override fun Project.createExtension() = extensions.create<BenchmarkExtension>(EXTENSION_NAME, this)

    private val Project.linkTaskProvider: TaskProvider<out KotlinNativeLink>
        get() = hostKotlinNativeTarget.binaries.getExecutable(NATIVE_EXECUTABLE_NAME, project.buildType).linkTaskProvider

    override fun Project.createNativeBinary(target: KotlinNativeTarget) {
        target.binaries.executable(NATIVE_EXECUTABLE_NAME, listOf(project.buildType)) {
            this.runTaskProvider?.configure {
                group = ""
                enabled = false
            }
        }
    }

    override fun RunKotlinNativeTask.configureKonanRunTask() {
        executable.fileProvider(project.linkTaskProvider.map { it.outputFile.get() })
    }

    override fun JsonReportTask.configureKonanJsonReportTask() {
        codeSizeBinary.fileProvider(project.linkTaskProvider.map { it.outputFile.get() })
        compilerFlags.addAll(project.linkTaskProvider.map { it.toolOptions.freeCompilerArgs.get() })
    }
}
