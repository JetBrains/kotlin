package org.jetbrains.kotlin.konan

import java.io.*
import java.net.URL
import java.util.Properties
import kotlin.concurrent.thread

// TODO: Try to use some dependency management system (Ivy?)
class DependencyDownloader(dependenciesRoot: File, val properties: Properties, val dependencies: List<String>) {

    val dependenciesDirectory = dependenciesRoot.apply { mkdirs() }
    val cacheDirectory = System.getProperty("user.home")?.let {
        File("$it/.konan/cache").apply { mkdirs() }
    } ?: dependenciesRoot

    val lockFile = File(cacheDirectory, ".lock").apply { if (!exists()) createNewFile() }

    val dependenciesUrl: String =
            properties.getProperty("dependenciesUrl", "https://jetbrains.bintray.com/kotlin-native-dependencies")

    var isInfoShown = false

    class DependencyFile(directory: File, fileName: String) {
        val file = File(directory, fileName).apply { createNewFile() }
        private val dependencies_ = file.readLines().toMutableSet()
        val dependencies = dependencies_.toSet()

        fun contains(dependency: String) = dependencies_.contains(dependency)
        fun add(dependency: String) = dependencies_.add(dependency)
        fun addWithSave(dependency: String) {
            add(dependency)
            save()
        }

        fun save() {
            val writer = file.writer()
            writer.use {
                dependencies_.forEach {
                    writer.write(it)
                    writer.write("\n")
                }
            }
        }
    }

    private fun processDependency(dependency: String) {
        val depDir = File(dependenciesDirectory, dependency)
        val depName = depDir.name

        val cachedDependencies = DependencyFile(cacheDirectory, ".cached")
        val extractedDependencies = DependencyFile(dependenciesDirectory, ".extracted")
        if (extractedDependencies.contains(depName) &&
                depDir.exists() &&
                depDir.isDirectory &&
                depDir.list().isNotEmpty()) {
            return
        }

        if (!isInfoShown) {
            println("Downloading native dependencies (LLVM, sysroot etc). This is one-time action performing only for the first run of the compiler.")
            isInfoShown = true
        }

        val archive = File(cacheDirectory.canonicalPath, "$depName.tar.gz")
        if (!cachedDependencies.contains(depName) || !archive.exists()) {
            download(depName, archive)
            cachedDependencies.addWithSave(depName)
        }
        extract(archive, dependenciesDirectory)
        extractedDependencies.addWithSave(depName)
    }

    private fun extract(tarGz: File, target: File) {
        println("Extract dependency: ${tarGz.canonicalPath} in ${target.canonicalPath}")
        val tarProcess = ProcessBuilder().apply {
            command("tar", "-xzf", "${tarGz.canonicalPath}")
            directory(target)
        }.start()
        tarProcess.waitFor()
        if (tarProcess.exitValue() != 0) {
            throw RuntimeException("Cannot extract archive with dependency: ${tarGz.canonicalPath}")
        }
    }

    private val Long.humanReadable: String
        get() {
            if (this < 0) {
                return "-"
            }
            if (this < 1024) {
                return "$this bytes"
            }
            val exp = (Math.log(this.toDouble()) / Math.log(1024.0)).toInt()
            val prefix = "kMGTPE"[exp-1]
            return "%.1f %sB".format(this / Math.pow(1024.0, exp.toDouble()), prefix)
        }

    private fun updateProgressMsg(url: String, currentBytes: Long, totalBytes: Long) {
        print("\rDownload dependency: $url (${currentBytes.humanReadable}/${totalBytes.humanReadable}). ")
    }

    private fun download(dependencyName: String, outputFile: File) {
        if (!outputFile.exists()) {
            val url = URL("$dependenciesUrl/$dependencyName.tar.gz")
            val connection = url.openConnection()
            val totalBytes = connection.contentLengthLong

            var currentBytes = 0L
            var done = false
            var downloadError: Throwable? = null

            thread {
                try {
                    url.openStream().use { from ->
                        outputFile.outputStream().use { to ->
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            var read = from.read(buffer)
                            while (read != -1) {
                                to.write(buffer, 0, read)
                                currentBytes += read
                                read = from.read(buffer)
                            }
                        }
                    }
                } catch (e: Throwable) {
                    downloadError = e
                }
                done = true
            }

            // TODO: Improve console logging
            while (!done) {
                Thread.sleep(1000) // We can use condition variable here.
                updateProgressMsg(url.toString(), currentBytes, totalBytes)
            }
            println("Done.")
            if (downloadError != null) {
                throw RuntimeException("Cannot download dependency: $url", downloadError)
            }
        }
    }

    fun run() {
        val systemLock = RandomAccessFile(lockFile, "rw").channel.lock()
        dependencies.forEach {
            processDependency(it)
        }
        systemLock.release()
    }
}