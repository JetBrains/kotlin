package org.jetbrains.kotlin

import org.gradle.api.Project
import org.gradle.internal.extensions.core.extra
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import java.io.File
import java.util.*

/**
 * Native distribution used for building the code.
 */
val Project.kotlinNativeHome: File
    get() = rootProject.file(property("kotlin.native.home") as String)

/**
 * How many warmup iterations should each benchmark do
 */
val Project.nativeWarmup: Int
    get() = (property("nativeWarmup") as String).toInt()

/**
 * How many iterations (excluding [nativeWarmup]) should each benchmark do
 */
val Project.attempts: Int
    get() = (property("attempts") as String).toInt()

/**
 * When set, the benchmarks won't be executed, but everything will be built.
 *
 * This can be used prior to running many benchmarks to make sure no building task
 * interferes with the benchmarks execution
 */
val Project.dryRun: Boolean
    get() = (findProperty("dryRun") as String?)?.let { it.isEmpty() || it == "true" } ?: false

/**
 * Space-separated additional compiler arguments for each benchmark
 */
val Project.compilerArgs: List<String>
    get() = (findProperty("compilerArgs") as String?)?.split("\\s".toRegex()).orEmpty()

/**
 * Comma-separated list of benchmarks to run
 *
 * @see filterRegex
 */
val Project.filter: String?
    get() = project.findProperty("filter") as String?

/**
 * Comma-separated list of benchmarks (described by regular expressions) to run
 *
 * @see filter
 */
val Project.filterRegex: String?
    get() = project.findProperty("filterRegex") as String?

/**
 * List of all known benchmark groups.
 */
@Suppress("UNCHECKED_CAST")
val Project.knownGroups: List<String>
    get() = extra["knownGroups"] as List<String>

/**
 * List of benchmark groups to include.
 */
val Project.groups: List<String>
    get() = (project.findProperty("groups") as String?)
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.takeIf { it.isNotEmpty() }
            ?: knownGroups

/**
 * Compiler version to store in the generated reports.
 *
 * On CI uses the `build.number` property.
 * Locally extracts the `compilerVersion` from the [provided distribution][kotlinNativeHome].
 */
val Project.compilerVersion: String
    get() = (findProperty("build.number") as String?) ?: kotlinNativeHome.resolve("konan/konan.properties").let {
        Properties().apply {
            load(it.reader())
        }["compilerVersion"] as String
    }

/**
 * File name for the benchmark report.
 */
val Project.nativeJson: String
    get() = project.property("nativeJson") as String

/**
 * How to build Native code: debug or release.
 *
 * Default: release
 */
val Project.buildType: NativeBuildType
    get() = (findProperty("nativeBuildType") as String?)?.let { NativeBuildType.valueOf(it) } ?: NativeBuildType.RELEASE

/**
 * If `true`, wrap benchmarking code with `cset` to keep it tied to a single core.
 *
 * For certain (very-very micro) benchmarks, it may improve stability of measurements.
 */
val Project.useCSet: Boolean
    get() = (findProperty("useCSet") as String?).toBoolean()

/**
 * Run only a predefined set of benchmarks
 */
val Project.baseOnly: Boolean
    get() = (findProperty("baseOnly") as String?).toBoolean()