/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nativeDistribution

import bootstrapKotlinVersion
import org.gradle.api.Project
import org.gradle.api.file.*
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Sync
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.PlatformInfo
import org.jetbrains.kotlin.konan.util.ArchiveType
import org.jetbrains.kotlin.konan.util.ArchiveType.TAR_GZ
import org.jetbrains.kotlin.konan.util.ArchiveType.ZIP
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
fun Project.registerNativeReleasedDistribution(version: String): Provider<NativeDistribution> {
    val configuration = releasedNativeDistributionConfiguration(version)
    val distributionFiles = configuration.incoming.files

    /*
    Setup a 'sync' task to unpack the native distribution archive.
    We're using a sync into the build directory in favor of any artifact transform, because this native
    distribution is ment to used, as is, for further builds. Such builds might store files within the distribution
    which would violate Gradle invariants for artifact transforms (which are ment to stay immutable).
     */
    val syncTaskName = "syncNativeDistributionV$version"
    val syncTask = if (syncTaskName !in tasks.names) tasks.register<Sync>(syncTaskName) {
        from(project.files({
            distributionFiles.map { archive ->
                when {
                    archive.path.endsWith("." + TAR_GZ.fileExtension) -> tarTree(archive)
                    archive.path.endsWith("." + ZIP.fileExtension) -> zipTree(archive)
                    else -> error("Unsupported archive type: $archive")
                }
            }
        }).builtBy(distributionFiles)) {
            eachFile {
                relativePath = RelativePath(true, *relativePath.segments.drop(1).toTypedArray())
            }
            includeEmptyDirs = false
        }
        into(layout.buildDirectory.dir("nativeDistributionV$version"))
    } else tasks.named<Sync>(syncTaskName)

    return syncTask.map { NativeDistribution(project.layout.buildDirectory.dir("nativeDistributionV$version").get()) }
}

/**
 * Get Native bootstrap distribution.
 */
fun Project.registerNativeBootstrapDistribution(): Provider<NativeDistribution> =
        registerNativeReleasedDistribution(bootstrapKotlinVersion)
