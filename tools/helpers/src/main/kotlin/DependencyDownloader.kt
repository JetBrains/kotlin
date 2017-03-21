package org.jetbrains.kotlin.konan

import java.io.File
import java.io.RandomAccessFile
import java.net.URL
import java.nio.file.*
import java.util.*

// TODO: Try to use some dependency management system (Ivy?)
// TODO: Show % and size during downloading
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
            command("tar", "-x", "-f", "${tarGz.canonicalPath}")
            directory(tarGz.parentFile)
        }.start()
        tarProcess.waitFor()
        if (tarProcess.exitValue() != 0) {
            throw RuntimeException("Cannot extract archive with dependency: ${tarGz.canonicalPath}")
        }
    }

    private fun download(dependencyName: String): File {
        val to = File(dependenciesRoot.canonicalPath, "$dependencyName.tar.gz")
        if (!to.exists()) {
            val from = URL("$dependenciesUrl/$dependencyName.tar.gz")
            println("Download dependency: $dependencyName from $from")
            from.openStream().use {
                Files.copy(it, Paths.get(to.toURI()), StandardCopyOption.REPLACE_EXISTING)
            }
        }
        return to
    }

    fun run() {
        val systemLock = RandomAccessFile(lockFile, "rw").channel.lock()
        dependencies.forEach {
            processDependency(it)
        }
        systemLock.release()
    }
}