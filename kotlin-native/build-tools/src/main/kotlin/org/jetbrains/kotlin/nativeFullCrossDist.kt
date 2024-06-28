/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import kotlinBuildProperties
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.RelativePath
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File

/**
 * "crossBundle" stands for the K/N distribution (bundle) that contains all platform klibs, including
 * for cross-targets. Simply speaking, for MacOS "crossBundle" is the same as the usual "bundle", but for
 * Linux/Win it's a "bundle" + Darwin-specific klibs like `Foundation`.
 *
 * The process of building "crossBundle" is intuitively a merge of "Darwin-dist" ([PATH_TO_DARWIN_DIST_PROPERTY]) into
 * "Host-dist" ([PATH_TO_HOST_DIST_PROPERTY]) without overwriting.
 * To be more precise, we copy only klib/platform/<darwin-specific-targets> from Darwin-dist into Host-dist
 *
 * The result is packed into archive as usual `bundlePrebuilt` tasks do.
 *
 * Note that this task *DOES NOT USE HOST DIST BUILT FROM SOURCES*. However, it is expected (but not checked in the code)
 * that the passed [PATH_TO_HOST_DIST_PROPERTY] will point to the host-bundle built from the exact same source revision
 * as the one current build uses.
 *
 * We use such splitting instead of depending directly on usual `bundlePrebuilt` because this allows to run usual bundles in parallel
 * on TC and then merge them into cross-bundle quickly (rather than first build MacOS bundle and only then start building Linux bundle).
 */
fun Project.setupMergeCrossBundleTask(): TaskProvider<Task>? {
    val pathToDarwinDistProperty = pathToDarwinDistProperty
    val pathToHostDistProperty = pathToHostDistProperty
    if (pathToDarwinDistProperty == null || pathToHostDistProperty == null) return null

    val checkPreconditions = tasks.register("checkCrossDistPreconditions") {
        doLast { requireCrossDistEnabled(pathToDarwinDistProperty, pathToHostDistProperty) }
    }

    // Host dist is unpacked straight to kotlin-native/dist
    val unpackHostDist = setupTaskToUnpackDistToCurrentDist(
            distName = "Host",
            source = hostDistFile,
            dependency = checkPreconditions,
            // Licenses are configured separately in bundlePrebuilt/bundleRegular tasks, which will depend
            // on mergeCrossBundle (look for 'configurePackingLicensesToBundle' in kotlin-native/build.gradle)
            excludes = listOf("*/licenses/**")
    )

    val unpackDarwinDist = setupTaskToUnpackDistToCurrentDist(
            distName = "Darwin",
            source = darwinDistFile,
            dependency = checkPreconditions,
            includes = getDarwinOnlyTargets().map { "*/klib/platform/${it.name}/**" }
    )

    return tasks.register("mergeCrossBundle") {
        dependsOn(unpackHostDist, unpackDarwinDist)
    }
}

private fun Project.setupTaskToUnpackDistToCurrentDist(
        distName: String,
        source: File,
        dependency: TaskProvider<*>,
        includes: Collection<String>? = null,
        excludes: Collection<String>? = null,
): TaskProvider<Copy> = tasks.register<Copy>("unpack${distName}Dist") {
    from(tarTree(source)) {
        if (includes != null) include(includes)
        if (excludes != null) exclude(excludes)
    }
    into(kotlinNativeDist)
    dependsOn(dependency)

    // this incantation makes it so that we will have something like build/unpackedDonorDarwinDist/<contents of dist>
    // rather than build/unpackedDonorDarwinDist/kotlin-native-macos-aarch65-2.0.255-SNAPSHOT/<contents of dist>
    eachFile { relativePath = RelativePath(true, *relativePath.segments.drop(1).toTypedArray<String?>()) }
    includeEmptyDirs = false
}

// Assume that all Macs have the same enabled targets, and that Darwin dist is exactly a superset of any other dist
// TODO(KT-67686) Expose proper API here
fun getDarwinOnlyTargets(): Set<KonanTarget> {
    val enabledByHost = HostManager().enabledByHost
    val enabledOnMacosX64 = enabledByHost[KonanTarget.MACOS_X64]!!.toSet()
    val enabledOnMacosArm64 = enabledByHost[KonanTarget.MACOS_ARM64]!!.toSet()

    // Assert the assumption: all macs have the same enabled targets
    require(enabledOnMacosArm64 == enabledOnMacosX64) {
        val symmetricalDifference = (enabledOnMacosArm64 union enabledOnMacosX64) - (enabledOnMacosArm64 intersect enabledOnMacosX64)
        "Attention! Enabled targets for macosArm64 and macosX64 are different!\n" +
                "Diff = $symmetricalDifference\n" +
                "Please, revise the code for building Full K/N Cross-Dist"
    }

    val enabledOnThisHost = enabledByHost[HostManager.host]!!.toSet()
    return enabledOnMacosX64 subtract enabledOnThisHost
}

private val Project.pathToDarwinDistProperty: String?
    get() = kotlinBuildProperties.getOrNull(PATH_TO_DARWIN_DIST_PROPERTY) as? String
private val Project.pathToHostDistProperty: String?
    get() = kotlinBuildProperties.getOrNull(PATH_TO_HOST_DIST_PROPERTY) as? String

private val Project.darwinDistFile: File
    get() = rootProject.rootDir.resolve(File(pathToDarwinDistProperty!!))
private val Project.hostDistFile: File
    get() = rootProject.rootDir.resolve(File(pathToHostDistProperty!!))

private fun requireCrossDistEnabled(pathToDarwinDistProperty: String?, pathToHostDistProperty: String?) {
    fun checkDist(propertyName: String, propertyValue: String?) {
        requireNotNull(propertyValue) { "Full cross-dist is not enabled, $propertyName is not provided" }
        val distFile = File(propertyValue)

        require(distFile.exists()) { "$propertyName is specified, but points to non-existing file: ${distFile.absolutePath}" }
        require(distFile.isFile) {
            "Expected to receive path to packed dist, but $propertyName is specified and points to a non-file ${distFile.absolutePath}. " +
                    "Have you passed a path to the folder with unpacked dist?"
        }
    }

    checkDist(PATH_TO_DARWIN_DIST_PROPERTY, pathToDarwinDistProperty)
    checkDist(PATH_TO_HOST_DIST_PROPERTY, pathToHostDistProperty)
}

const val PATH_TO_DARWIN_DIST_PROPERTY = "kotlin.native.pathToDarwinDist"
const val PATH_TO_HOST_DIST_PROPERTY = "kotlin.native.pathToHostDist"
