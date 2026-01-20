package org.jetbrains.kotlin.benchmark

import org.gradle.api.Project
import org.jetbrains.kotlin.RunKotlinNativeTask
import org.jetbrains.kotlin.gradle.plugin.mpp.Executable
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

    private val Project.nativeBinary: Executable
        get() = hostKotlinNativeTarget
            .binaries.getExecutable(NATIVE_EXECUTABLE_NAME, benchmark.buildType)

    override val Project.nativeLinkBinary: String
        get() = nativeBinary.outputFile.absolutePath

    override val Project.nativeLinkTaskArguments: List<String>
        get() {
            return nativeBinary.linkTaskProvider.get().toolOptions.freeCompilerArgs.get()
        }

    override fun RunKotlinNativeTask.configureKonanRunTask() {
        executable.fileProvider(project.nativeBinary.linkTaskProvider.map { it.outputFile.get() })
    }
}
