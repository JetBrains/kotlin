/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.target

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.properties.Properties
import org.jetbrains.kotlin.konan.properties.keepOnlyDefaultProfiles
import org.jetbrains.kotlin.konan.properties.loadProperties
import org.jetbrains.kotlin.konan.util.DependencyDirectories

class Distribution(
        val konanHome: String,
        private val onlyDefaultProfiles: Boolean = false,
        private val runtimeFileOverride: String? = null,
        private val propertyOverrides: Map<String, String>? = null
) {

    val localKonanDir = DependencyDirectories.localKonanDir

    val konanSubdir = "$konanHome/konan"
    val mainPropertyFileName = "$konanSubdir/konan.properties"
    val experimentalEnabled by lazy {
        File("$konanSubdir/experimentalTargetsEnabled").exists
    }

    private fun propertyFilesFromConfigDir(configDir: String, genericName: String): List<File> {
        val directory = File(configDir, "platforms/$genericName")
        return if (directory.isDirectory)
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

    /**
     * Please note that konan.properties uses simple resolving mechanism.
     * See [org.jetbrains.kotlin.konan.properties.resolveValue].
     */
    val properties by lazy {
        val result = Properties()

        fun loadPropertiesSafely(source: File) {
            if (source.isFile) result.putAll(source.loadProperties())
        }

        loadPropertiesSafely(File(mainPropertyFileName))

        HostManager.knownTargetTemplates.forEach { targetTemplate ->
            additionalPropertyFiles(targetTemplate).forEach {
                loadPropertiesSafely(it)
            }
        }

        if (onlyDefaultProfiles) {
            result.keepOnlyDefaultProfiles()
        }
        propertyOverrides?.let(result::putAll)
        result
    }

    val compilerVersion by lazy {
        val propertyVersion = properties["compilerVersion"]?.toString()
        val bundleVersion = if (konanHome.contains("-1"))
            konanHome.substring(konanHome.lastIndexOf("-1") + 1)
        else
            null
        propertyVersion ?: bundleVersion
    }

    val klib = "$konanHome/klib"
    val stdlib = "$klib/common/stdlib"
    val stdlibDefaultComponent = "$stdlib/default"

    fun defaultNatives(target: KonanTarget) = "$konanHome/konan/targets/${target.visibleName}/native"

    fun runtime(target: KonanTarget) = runtimeFileOverride ?: "$stdlibDefaultComponent/targets/${target.visibleName}/native/runtime.bc"

    fun platformDefs(target: KonanTarget) = "$konanHome/konan/platformDef/${target.visibleName}"

    fun platformLibs(target: KonanTarget) = "$klib/platform/${target.visibleName}"

    val launcherFiles = listOf("launcher.bc")

    val dependenciesDir = DependencyDirectories.defaultDependenciesRoot.absolutePath

    val subTargetProvider = object: SubTargetProvider {
        override fun availableSubTarget(genericName: String) =
                additionalPropertyFiles(genericName).map { it.name }
    }
}

// TODO: Move into K/N?
fun buildDistribution(konanHome: String) = Distribution(konanHome,true, null)

fun customerDistribution(konanHome: String) = Distribution(konanHome,false, null)