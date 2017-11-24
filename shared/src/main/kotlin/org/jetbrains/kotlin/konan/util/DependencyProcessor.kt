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

package org.jetbrains.kotlin.konan.util

import org.jetbrains.kotlin.konan.file.use
import org.jetbrains.kotlin.konan.properties.KonanProperties
import org.jetbrains.kotlin.konan.properties.Properties
import java.io.File
import java.io.FileNotFoundException
import java.io.RandomAccessFile
import java.net.URL
import java.nio.file.Paths
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private val Properties.dependenciesUrl : String
    get() = getProperty("dependenciesUrl")
            ?: throw IllegalStateException("No such property in konan.properties: dependenciesUrl")

private val Properties.airplaneMode : Boolean
    get() = getProperty("airplaneMode")?.toBoolean() ?: false

private val Properties.downloadingAttempts : Int
    get() = getProperty("downloadingAttempts")?.toInt()
            ?: DependencyDownloader.DEFAULT_MAX_ATTEMPTS

private val Properties.downloadingAttemptIntervalMs : Long
    get() = getProperty("downloadingAttemptPauseMs")?.toLong()
            ?: DependencyDownloader.DEFAULT_ATTEMPT_INTERVAL_MS

private val Properties.homeDependencyCache : String
    get() = getProperty("homeDependencyCache") ?: DependencyProcessor.DEFAULT_HOME_DEPENDENCY_CACHE


private val KonanProperties.dependenciesUrl : String            get() = properties.dependenciesUrl
private val KonanProperties.airplaneMode : Boolean              get() = properties.airplaneMode
private val KonanProperties.downloadingAttempts : Int           get() = properties.downloadingAttempts
private val KonanProperties.downloadingAttemptIntervalMs : Long get() = properties.downloadingAttemptIntervalMs
private val KonanProperties.homeDependencyCache : String        get() = properties.homeDependencyCache

// TODO: Try to use some dependency management system (Ivy?)
// TODO: Maybe rename.
/**
 * Inspects [dependencies] and downloads all the missing ones into [dependenciesDirectory] from [dependenciesUrl].
 * If [airplaneMode] is true will throw a RuntimeException instead of downloading.
 */
class DependencyProcessor(dependenciesRoot: File,
                          val dependenciesUrl: String,
                          val dependencies: Collection<String>,
                          homeDependencyCache: String = DEFAULT_HOME_DEPENDENCY_CACHE,
                          val airplaneMode: Boolean = false,
                          maxAttempts: Int = DependencyDownloader.DEFAULT_MAX_ATTEMPTS,
                          attemptIntervalMs: Long = DependencyDownloader.DEFAULT_ATTEMPT_INTERVAL_MS,
                          customProgressCallback: ProgressCallback? = null) {

    val dependenciesDirectory = dependenciesRoot.apply { mkdirs() }
    val cacheDirectory = System.getProperty("user.home")?.let {
        Paths.get(it).resolve(homeDependencyCache).toFile().apply { mkdirs() }
    } ?: dependenciesRoot

    val lockFile = File(cacheDirectory, ".lock").apply { if (!exists()) createNewFile() }

    var showInfo = true
    private var isInfoShown = false

    // TOOO: Rename pause -> interval
    private val downloader = DependencyDownloader(maxAttempts, attemptIntervalMs, customProgressCallback)
    private val extractor = DependencyExtractor()

    private val archiveExtension get() = extractor.archiveExtension

    constructor(dependenciesRoot: File,
                properties: KonanProperties,
                dependenciesUrl: String = properties.dependenciesUrl) : this(
            dependenciesRoot,
            properties.properties,
            properties.dependencies,
            dependenciesUrl)

    constructor(dependenciesRoot: File,
                properties: Properties,
                dependencies: List<String>,
                dependenciesUrl: String = properties.dependenciesUrl) : this(
            dependenciesRoot,
            dependenciesUrl,
            dependencies,
            airplaneMode = properties.airplaneMode,
            maxAttempts = properties.downloadingAttempts,
            attemptIntervalMs = properties.downloadingAttemptIntervalMs)


    class DependencyFile(directory: File, fileName: String) {
        val file = File(directory, fileName).apply { createNewFile() }
        private val dependencies = file.readLines().toMutableSet()

        fun contains(dependency: String) = dependencies.contains(dependency)
        fun add(dependency: String) = dependencies.add(dependency)
        fun remove(dependency: String) = dependencies.remove(dependency)

        fun removeAndSave(dependency: String) {
            remove(dependency)
            save()
        }

        fun addAndSave(dependency: String) {
            add(dependency)
            save()
        }

        fun save() {
            val writer = file.writer()
            writer.use {
                dependencies.forEach {
                    writer.write(it)
                    writer.write("\n")
                }
            }
        }
    }

    private fun processDependency(dependency: String) {
        val depDir = File(dependenciesDirectory, dependency)
        val depName = depDir.name

        val fileName = "$depName.$archiveExtension"
        val archive = cacheDirectory.resolve(fileName)
        val url = URL("$dependenciesUrl/$fileName")

        val extractedDependencies = DependencyFile(dependenciesDirectory, ".extracted")
        if (extractedDependencies.contains(depName) &&
            depDir.exists() &&
            depDir.isDirectory &&
            depDir.list().isNotEmpty()) {

            if (depDir.list().contains(".unstable")) {
                // The downloaded version of the dependency is unstable -> redownload it.
                depDir.deleteRecursively()
                archive.delete()
                extractedDependencies.removeAndSave(dependency)
            } else {
                return
            }
        }

        if (showInfo && !isInfoShown) {
            println("Downloading native dependencies (LLVM, sysroot etc). This is a one-time action performed only on the first run of the compiler.")
            isInfoShown = true
        }

        if (!archive.exists()) {
            if (airplaneMode) {
                throw FileNotFoundException("""
                    Cannot find a dependency locally: $dependency.
                    Set `airplaneMode = false` in konan.properties to download it.
                """.trimIndent())
            }
            downloader.download(url, archive)
        }
        println("Extracting dependency: $archive into $dependenciesDirectory")
        extractor.extract(archive, dependenciesDirectory)
        extractedDependencies.addAndSave(depName)
    }

    companion object {
        private val lock = ReentrantLock()

        const val DEFAULT_HOME_DEPENDENCY_CACHE = ".konan/cache"

        @JvmStatic
        val defaultDependenciesRoot
            get() = Paths.get(System.getProperty("user.home")).resolve(".konan/dependencies").toFile()
    }

    fun run() = lock.withLock {
        RandomAccessFile(lockFile, "rw").channel.lock().use {
            dependencies.forEach {
                processDependency(it)
            }
        }
    }
}
