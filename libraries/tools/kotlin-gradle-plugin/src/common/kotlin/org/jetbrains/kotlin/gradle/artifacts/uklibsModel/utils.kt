package org.jetbrains.kotlin

import org.jetbrains.kotlin.incremental.deleteDirectoryContents
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

fun zipFragments(
    manifest: String,
    fragmentToArtifact: Map<String, File>,
    outputZip: File,
    temporariesDirectory: File,
) {
    ZipOutputStream(BufferedOutputStream(FileOutputStream(outputZip))).use { zos ->
        fragmentToArtifact.forEach { (identifier, file) ->
            // Unpacked metadata classes
            if (file.isDirectory) {
                packDirectory(file, identifier, zos)
            } else if (file.extension == "klib") {
                val temp = temporariesDirectory.resolve(identifier)
                if (temp.exists()) temp.deleteDirectoryContents()
                temp.mkdirs()
                unzip(
                    zipFilePath = file,
                    outputFolderPath = temp,
                )
                packDirectory(
                    directory = temp,
                    identifier = identifier,
                    zos = zos,
                )
            } else {
                error("Trying to pack invalid file in uklib: ${file}")
            }
        }
        zos.putNextEntry(ZipEntry("umanifest"))
        manifest.byteInputStream().copyTo(zos)
        zos.closeEntry()
    }
}

private fun packDirectory(
    directory: File,
    identifier: String,
    zos: ZipOutputStream
) {
    Files.walk(directory.toPath()).forEach { path ->
        val zipEntry = ZipEntry(identifier + "/" + path.toFile().toRelativeString(directory))
        if (!Files.isDirectory(path)) {
            zos.putNextEntry(zipEntry)
            Files.newInputStream(path).use { inputStream ->
                inputStream.copyTo(zos)
            }
            zos.closeEntry()
        }
    }
}

fun unzip(zipFilePath: File, outputFolderPath: File) {
    val buffer = ByteArray(1024)
    ZipInputStream(FileInputStream(zipFilePath)).use { zis ->
        var zipEntry: ZipEntry? = zis.nextEntry
        while (zipEntry != null) {
            val newFile = File(outputFolderPath, zipEntry.name)
            if (zipEntry.isDirectory) {
                newFile.mkdirs()
            } else {
                newFile.parentFile?.mkdirs()
                FileOutputStream(newFile).use { fos ->
                    zis.copyTo(fos)
                }
            }
            zipEntry = zis.nextEntry
        }
        zis.closeEntry()
    }
}

