package org.jetbrains.kotlin.benchmark

import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.JsonReportTask
import org.jetbrains.kotlin.RunKotlinNativeTask
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
import org.jetbrains.kotlin.hostKotlinNativeTarget
import kotlin.reflect.KClass

/**
 * A plugin configuring a benchmark Kotlin/Native project.
 */
open class KotlinNativeBenchmarkingPlugin: BenchmarkingPlugin() {

    override val benchmarkExtensionClass: KClass<*>
        get() = BenchmarkExtension::class

    override val Project.benchmark: BenchmarkExtension
        get() = extensions.getByName(benchmarkExtensionName) as BenchmarkExtension

    override val benchmarkExtensionName: String = "benchmark"

    private val Project.linkTaskProvider: TaskProvider<out KotlinNativeLink>
        get() = hostKotlinNativeTarget.binaries.getExecutable(NATIVE_EXECUTABLE_NAME, benchmark.buildType).linkTaskProvider

    override fun RunKotlinNativeTask.configureKonanRunTask() {
        executable.fileProvider(project.linkTaskProvider.map { it.outputFile.get() })
    }

    override fun JsonReportTask.configureKonanJsonReportTask() {
        codeSizeBinary.fileProvider(project.linkTaskProvider.map { it.outputFile.get() })
        compilerFlags.addAll(project.linkTaskProvider.map { it.toolOptions.freeCompilerArgs.get() })
    }
}
