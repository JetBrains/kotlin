package org.jetbrains.kotlin.konan

import java.io.*
import java.net.URL
import java.util.Properties
import kotlin.concurrent.thread

// TODO: Try to use some dependency management system (Ivy?)
class DependencyDownloader(dependenciesRoot: File, val properties: Properties, val dependencies: List<String>) {

    val dependenciesRoot = dependenciesRoot.apply { mkdirs() }

    val lockFile = File(dependenciesRoot, ".lock").apply { createNewFile() }
    val listFile = File(dependenciesRoot, ".downloaded")

    val dependenciesUrl: String =
            properties.getProperty("dependenciesUrl", "https://jetbrains.bintray.com/kotlin-native-dependencies")

    var isInfoShown = false

    private fun File.containsLine(line: String): Boolean {
        if (!exists()) {
            return false
        }
        var result = false
        listFile.forEachLine {
            if (!result && it == line) {
                result = true
            }
        }
        return result
    }

    private fun processDependency(path: String) {
        val depDir = File(path)
        val depName = depDir.name
        val inListFile = listFile.containsLine(depName)
        if (inListFile && depDir.exists()) {
            return
        }
        if (!isInfoShown) {
            println("Downloading native dependencies (LLVM, sysroot etc). This is one-time action performing only for the first run of the compiler.")
            isInfoShown = true
        }
        val downloaded = download(depName)
        println("Extract dependency: $downloaded -> $depDir")
        extract(downloaded)
        if (!inListFile) {
            listFile.appendText("$depName\n")
        }
    }

    private fun extract(tarGz: File) {
        val tarProcess = ProcessBuilder().apply {
            command("tar", "-xzf", "${tarGz.canonicalPath}")
            directory(tarGz.parentFile)
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

    private fun download(dependencyName: String): File {
        val outputFile = File(dependenciesRoot.canonicalPath, "$dependencyName.tar.gz")
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
            var msgLen = 0
            while (!done) {
                Thread.sleep(1000) // We can use condition variable here.
                updateProgressMsg(url.toString(), currentBytes, totalBytes)
            }
            println("Done.")
            if (downloadError != null) {
                throw RuntimeException("Cannot download dependency: $url", downloadError)
            }
        }
        return outputFile
    }

    fun run() {
        val systemLock = RandomAccessFile(lockFile, "rw").channel.lock()
        dependencies.forEach {
            processDependency(it)
        }
        systemLock.release()
    }
}