package org.jetbrains.kotlin

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
//import kotlin.io.path.relativeTo

fun zipFragments(
    manifest: String,
    fragmentToArtifact: Map<String, File>,
    outputZip: File
) {
    ZipOutputStream(BufferedOutputStream(FileOutputStream(outputZip))).use { zos ->
        fragmentToArtifact.forEach { (identifier, directory) ->
            Files.walk(directory.toPath()).forEach { path ->
                val zipEntry = ZipEntry(identifier + "/" + path.relativize(directory.toPath()).toString())
                if (!Files.isDirectory(path)) {
                    zos.putNextEntry(zipEntry)
                    Files.newInputStream(path).use { inputStream ->
                        inputStream.copyTo(zos)
                    }
                    zos.closeEntry()
                }
            }
        }
        zos.putNextEntry(ZipEntry("umanifest"))
        manifest.byteInputStream().copyTo(zos)
        zos.closeEntry()
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

