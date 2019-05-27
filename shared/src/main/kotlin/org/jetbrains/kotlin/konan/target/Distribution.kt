/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.konan.target

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.properties.keepOnlyDefaultProfiles
import org.jetbrains.kotlin.konan.properties.loadProperties
import org.jetbrains.kotlin.konan.util.DependencyProcessor

class Distribution(
        private val onlyDefaultProfiles: Boolean = false,
        private val konanHomeOverride: String? = null,
        private val runtimeFileOverride: String? = null) {

    val localKonanDir = DependencyProcessor.localKonanDir

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

    val dependenciesDir = DependencyProcessor.defaultDependenciesRoot.absolutePath

    fun availableSubTarget(genericName: String) =
            additionalPropertyFiles(genericName).map { it.name }
}

fun buildDistribution(konanHomeOverride: String? = null) = Distribution(true, konanHomeOverride, null)

fun customerDistribution(konanHomeOverride: String? = null) = Distribution(false, konanHomeOverride, null)
