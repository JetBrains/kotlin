/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nativeDistribution

import bootstrapKotlinVersion
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Sync
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.PlatformInfo
import org.jetbrains.kotlin.kotlinNativeDist

/**
 * Describes distribution of Native compiler rooted at [root].
 */
// TODO: Make an inline class after groovy buildscripts are gone.
class NativeDistribution(val root: Directory) {
    /**
     * Directory with compiler jars.
     *
     * @see compilerClasspath
     */
    val compilerJars: Directory
        get() = root.dir("konan/lib")

    /**
     * Directory with CLI tools.
     *
     * @see cinterop
     */
    val bin: Directory
        get() = root.dir("bin")

    /**
     * Directory with libraries for the compiler itself (e.g. bridges for LLVM bindings).
     */
    val nativeLibs: Directory
        get() = root.dir("konan/nativelib")

    /**
     * Root directory for compiler caches.
     *
     * @see cache
     * @see stdlibCache
     */
    val cachesRoot: Directory
        get() = root.dir("klib/cache")

    /**
     * Directory with distribution sources.
     *
     * @see stdlibSources
     */
    val sources: Directory
        get() = root.dir("sources")

    /**
     * Directory with .def files for platform libs.
     */
    val platformLibsDefinitions: Directory
        get() = root.dir("konan/platformDef")

    /**
     * Directory with misc tools.
     */
    val tools: Directory
        get() = root.dir("tools")

    /**
     * Directory with Swift Export support.
     */
    val swiftExport: Directory
        get() = root.dir("konan/swift_export")

    /**
     * Directory with all platform libs KLIBs for a specific [target].
     */
    fun platformLibs(target: String): Directory = root.dir("klib/platform/$target")

    /**
     * Classpath in which it's possible to run K/N compiler.
     */
    val compilerClasspath: FileCollection
        get() = compilerJars.asFileTree.matching {
            include("kotlin-native-compiler-embeddable.jar")
        }

    /**
     * Additional platform configuration.
     */
    val konanPlatforms: Directory
        get() = root.dir("konan/platforms")

    /**
     * `konan.properties` file with targets and dependencies descriptions.
     */
    val konanProperties: RegularFile
        get() = root.file("konan/konan.properties")

    private fun bin(tool: String) = if (PlatformInfo.isWindows()) bin.file("$tool.bat") else bin.file(tool)

    /**
     * `run_konan` command line executable.
     */
    val runKonan: RegularFile
        get() = bin("run_konan")

    /**
     * `cinterop` command line executable.
     */
    val cinterop: RegularFile
        get() = bin("cinterop")

    /**
     * `klib` command line executable.
     */
    val klib: RegularFile
        get() = bin("klib")

    /**
     * Runtime files for a specific [target].
     */
    fun runtime(target: String): Directory = root.dir("konan/targets/$target/native")

    /**
     * Platform library [name] klib for a specific [target].
     */
    fun platformLib(name: String, target: String): Directory = platformLibs(target).dir(name)

    /**
     * Static compiler cache of library [name] for a specific [target].
     */
    fun cache(name: String, target: String): Directory = cachesRoot.dir("${target}-gSTATIC-system/${name}-cache")

    /**
     * Archive with stdlib sources.
     */
    val stdlibSources: RegularFile
        get() = sources.file("kotlin-stdlib-native-sources.zip")

    /**
     * Standard library klib.
     */
    val stdlib: Directory
        get() = root.dir("klib/common/stdlib")

    /**
     * Static compiler cache of standard library for a specific [target].
     */
    fun stdlibCache(target: String): Directory = cache(name = "stdlib", target)

    /**
     * Fingerprint of the contents of [compilerJars] and [nativeLibs].
     */
    val compilerFingerprint: RegularFile
        get() = root.file("konan/compiler.fingerprint")

    /**
     * Fingerprint of [runtime] contents for [target].
     */
    fun runtimeFingerprint(target: String) = root.file("konan/targets/$target/runtime.fingerprint")
}

fun DirectoryProperty.asNativeDistribution(): Provider<NativeDistribution> = this.map(::NativeDistribution)

/**
 * Get the default Native distribution location.
 *
 * There's no guarantee what's inside this distribution.
 * Use together with `dependsOn(":kotlin-native:<required-kind-of-distribution>").
 */
// TODO: Having tasks depend on this distribution is both errorprone
//       and sometimes incompatible with Gradle isolation mechanisms.
val Project.nativeDistribution: Provider<NativeDistribution>
    get() = layout.dir(provider { kotlinNativeDist }).map { NativeDistribution(it) }

/**
 * Get released Native distribution of [version].
 */
fun Project.nativeReleasedDistribution(version: String): Provider<NativeDistribution> {
    val configuration = releasedNativeDistributionConfiguration(version)
    val distributionFiles = configuration.incoming.files

    val syncTaskName = "syncNativeDistributionV$version"
    val syncTask = if (syncTaskName !in tasks.names) tasks.register<Sync>(syncTaskName) {
        from(distributionFiles)
        into(layout.buildDirectory.dir("nativeDistributionV$version"))
    } else tasks.named<Sync>(syncTaskName)

    return syncTask.map { NativeDistribution(project.layout.buildDirectory.dir("nativeDistributionV$version").get()) }
}

/**
 * Get Native bootstrap distribution.
 */
val Project.nativeBootstrapDistribution: Provider<NativeDistribution>
    get() = nativeReleasedDistribution(bootstrapKotlinVersion)
