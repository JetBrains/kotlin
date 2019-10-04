/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.target

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.properties.keepOnlyDefaultProfiles
import org.jetbrains.kotlin.konan.properties.loadProperties
import org.jetbrains.kotlin.konan.util.DependencyDirectories

class Distribution(
    private val onlyDefaultProfiles: Boolean = false,
    private val konanHomeOverride: String? = null,
    private val runtimeFileOverride: String? = null
) {

    val localKonanDir = DependencyDirectories.localKonanDir

    private fun findKonanHome(): String {
        if (konanHomeOverride != null) return konanHomeOverride

        val value = System.getProperty("konan.home", "dist")
        val path = File(value).absolutePath
        return path
    }

    val konanHome = findKonanHome()
    val konanSubdir = "$konanHome/konan"
    val mainPropertyFileName = "$konanSubdir/konan.properties"
    val experimentalEnabled by lazy {
        File("$konanSubdir/experimentalTargetsEnabled").exists
    }

    private fun propertyFilesFromConfigDir(configDir: String, genericName: String): List<File> {
        val directory = File(configDir, "platforms/$genericName")
        return if (directory.exists && directory.isDirectory)
            directory.listFiles
        else
            emptyList()
    }

    private fun preconfiguredPropertyFiles(genericName: String) =
        propertyFilesFromConfigDir(konanSubdir, genericName)

    private fun userPropertyFiles(genericName: String) =
        propertyFilesFromConfigDir(localKonanDir.absolutePath, genericName)

    fun additionalPropertyFiles(genericName: String) =
        preconfiguredPropertyFiles(genericName) + userPropertyFiles(genericName)

    val properties by lazy {
        val loaded = File(mainPropertyFileName).loadProperties()
        HostManager.knownTargetTemplates.forEach {
            additionalPropertyFiles(it).forEach {
                val additional = it.loadProperties()
                loaded.putAll(additional)
            }
        }
        if (onlyDefaultProfiles) {
            loaded.keepOnlyDefaultProfiles()
        }
        loaded
    }

    val klib = "$konanHome/klib"
    val stdlib = "$klib/common/stdlib"

    fun defaultNatives(target: KonanTarget) = "$konanHome/konan/targets/${target.visibleName}/native"

    fun runtime(target: KonanTarget) = runtimeFileOverride ?: "$stdlib/targets/${target.visibleName}/native/runtime.bc"

    val launcherFiles = listOf("launcher.bc")

    val dependenciesDir = DependencyDirectories.defaultDependenciesRoot.absolutePath

    fun availableSubTarget(genericName: String) =
        additionalPropertyFiles(genericName).map { it.name }
}

fun buildDistribution(konanHomeOverride: String? = null) = Distribution(true, konanHomeOverride, null)

fun customerDistribution(konanHomeOverride: String? = null) = Distribution(false, konanHomeOverride, null)