package org.jetbrains.kotlin

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.options.Option
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