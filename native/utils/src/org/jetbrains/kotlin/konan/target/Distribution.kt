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

class Distribution private constructor(private val serialized: Serialized) : java.io.Serializable {
    constructor(
        konanHome: String,
        onlyDefaultProfiles: Boolean = false,
        runtimeFileOverride: String? = null,
        propertyOverrides: Map<String, String>? = null,
        konanDataDir: String? = null
    ) : this(Serialized(konanHome, onlyDefaultProfiles, runtimeFileOverride, propertyOverrides, konanDataDir))

    val konanHome by serialized::konanHome
    private val onlyDefaultProfiles by serialized::onlyDefaultProfiles
    private val runtimeFileOverride by serialized::runtimeFileOverride
    private val propertyOverrides by serialized::propertyOverrides
    private val konanDataDir by serialized::konanDataDir

    val localKonanDir = DependencyDirectories.getLocalKonanDir(konanDataDir)

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

    /**
     * Consider using [org.jetbrains.kotlin.gradle.targets.native.KonanPropertiesBuildService] in case of Gradle.
     */
    val compilerVersion by lazy {
        getCompilerVersion(properties["compilerVersion"]?.toString(), konanHome)
    }

    val klib = "$konanHome/klib"
    val stdlib = "$klib/common/stdlib"
    val stdlibDefaultComponent = "$stdlib/default"

    fun defaultNatives(target: KonanTarget) = "$konanHome/konan/targets/${target.visibleName}/native"

    fun runtime(target: KonanTarget) = runtimeFileOverride ?: "$stdlibDefaultComponent/targets/${target.visibleName}/native/runtime.bc"

    fun compilerInterface(target: KonanTarget) =
        runtimeFileOverride ?: "$stdlibDefaultComponent/targets/${target.visibleName}/native/compiler_interface.bc"

    fun platformDefs(target: KonanTarget) = "$konanHome/konan/platformDef/${target.visibleName}"

    fun platformLibs(target: KonanTarget) = "$klib/platform/${target.visibleName}"

    val launcherFiles = listOf("launcher.bc")

    val dependenciesDir = DependencyDirectories
        .getDependenciesRoot(konanDataDir)
        .absolutePath

    val subTargetProvider = object: SubTargetProvider {
        override fun availableSubTarget(genericName: String) =
                additionalPropertyFiles(genericName).map { it.name }
    }

    companion object {
        /**
         * Try to guess compiler version using [konanHome].
         */
        private fun getBundleVersion(konanHome: String): String? =
            if (konanHome.contains("-1"))
                konanHome.substring(konanHome.lastIndexOf("-1") + 1)
            else
                null

        fun getCompilerVersion(propertyVersion: String?, konanHome: String): String? =
            propertyVersion ?: getBundleVersion(konanHome)
    }

    private fun writeReplace(): Any = serialized

    private data class Serialized(
        val konanHome: String,
        val onlyDefaultProfiles: Boolean,
        val runtimeFileOverride: String?,
        val propertyOverrides: Map<String, String>?,
        val konanDataDir: String?,
    ) : java.io.Serializable {
        companion object {
            private const val serialVersionUID: Long = 0L
        }

        private fun readResolve(): Any = Distribution(this)
    }
}

// TODO: Move into K/N?
fun buildDistribution(konanHome: String, konanDataDir: String? = null) = Distribution(konanHome,true, null, konanDataDir = konanDataDir)

fun customerDistribution(konanHome: String, konanDataDir: String? = null) = Distribution(konanHome,false, null, konanDataDir = konanDataDir)
