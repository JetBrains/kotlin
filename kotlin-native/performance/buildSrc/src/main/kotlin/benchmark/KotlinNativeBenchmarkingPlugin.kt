package org.jetbrains.kotlin.benchmark

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.Task
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.Executable
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.HostManager
import javax.inject.Inject
import kotlin.reflect.KClass

open class KotlinNativeBenchmarkExtension @Inject constructor(project: Project) : BenchmarkExtension(project) {
    var mingwSrcDirs: Collection<Any> = emptyList()
    var posixSrcDirs: Collection<Any> = emptyList()
}

/**
 * A plugin configuring a benchmark Kotlin/Native project.
 */
open class KotlinNativeBenchmarkingPlugin: BenchmarkingPlugin() {

    override val benchmarkExtensionClass: KClass<*>
        get() = KotlinNativeBenchmarkExtension::class

    override val Project.benchmark: KotlinNativeBenchmarkExtension
        get() = extensions.getByName(benchmarkExtensionName) as KotlinNativeBenchmarkExtension

    override val benchmarkExtensionName: String = "benchmark"

    private val Project.nativeBinary: Executable
        get() = (kotlin.targets.getByName(NATIVE_TARGET_NAME) as KotlinNativeTarget)
            .binaries.getExecutable(NATIVE_EXECUTABLE_NAME, benchmark.buildType)

    override val Project.nativeExecutable: String
        get() = nativeBinary.outputFile.absolutePath

    override val Project.nativeLinkTask: Task
        get() = nativeBinary.linkTaskProvider.get()

    override fun getCompilerFlags(project: Project, nativeTarget: KotlinNativeTarget) =
            super.getCompilerFlags(project, nativeTarget) + project.nativeBinary.freeCompilerArgs.map { "\"$it\"" }

    override fun NamedDomainObjectContainer<KotlinSourceSet>.configureSources(project: Project) {
        project.benchmark.let {
            commonMain.kotlin.srcDirs(*it.commonSrcDirs.toTypedArray())
            if (HostManager.hostIsMingw) {
                nativeMain.kotlin.srcDirs(*(it.nativeSrcDirs + it.mingwSrcDirs).toTypedArray())
            } else {
                nativeMain.kotlin.srcDirs(*(it.nativeSrcDirs + it.posixSrcDirs).toTypedArray())
            }
        }
    }

    companion object {
        const val BENCHMARK_EXTENSION_NAME = "benchmark"
    }
}
