/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.pill.combo.intellij

import org.jetbrains.kotlin.pill.xml
import java.io.File
import java.io.OutputStream
import java.nio.file.attribute.FileTime
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class IntellijRepoGenerator(kotlinProjectDir: File) : IntellijComboGeneratorBase(kotlinProjectDir) {
    companion object {
        const val GROUP_ID = "com.jetbrains.intellij.idea"
    }

    fun generate() {
        val repoPath = System.getProperty("pill.combo.intellij.repo.path", null) ?: error("Repository path is not specified")
        val version = System.getProperty("pill.combo.intellij.version", null) ?: error("Version is not specified")

        val repoDir = File(repoPath)
        check(repoDir.parentFile.isDirectory) { "Repository path parent does not exist" }

        if (!repoDir.deleteRecursively()) {
            error("Can not delete existing repository $repoDir")
        }

        val groupDir = File(repoDir, GROUP_ID.replace('.', '/'))

        fun getNameBase(artifactId: String) = "$artifactId/$version/$artifactId-$version"

        for (artifactId in substitutions.artifacts) {
            val substitutionsForArtifact = substitutions.getForArtifact(artifactId) ?: continue
            val nameBase = getNameBase(artifactId)
            val artifactFile = File(groupDir, "$nameBase.zip")
            artifactFile.parentFile.mkdirs()

            artifactFile.outputStream().use { fos ->
                ZipOutputStream(fos).use { zos ->
                    for (path in substitutionsForArtifact.keys) {
                        val entry = ZipEntry(path)
                        entry.setZeroDate()
                        zos.putNextEntry(entry)

                        if (path.toLowerCase().endsWith(".jar")) {
                            writeEmptyZipFile(zos)
                        }

                        zos.closeEntry()
                    }
                }
            }

            artifactFile.setZeroDate()

            writePom(File(groupDir, "$nameBase.pom"), artifactId, version, "zip")

            if (artifactId == "ideaIC" || artifactId == "ideaIU") {
                writeEmptyZipFile(File(groupDir, "$nameBase-sources.jar"))
            }
        }

        run /* jps-build-test */ {
            val artifactId = "jps-build-test"
            val nameBase = getNameBase(artifactId)

            val artifactFile = File(groupDir, "$nameBase.jar")
            artifactFile.parentFile.mkdirs()

            writeEmptyZipFile(artifactFile)
            writePom(File(groupDir, "$nameBase.pom"), artifactId, version, "jar")
        }
    }

    private fun writePom(file: File, artifactId: String, version: String, packaging: String) {
        file.writeText(
            xml("project") {
                xml("modelVersion") { raw("4.0.0") }
                xml("groupId") { raw(GROUP_ID) }
                xml("artifactId") { raw(artifactId) }
                xml("version") { raw(version) }
                xml("packaging") { raw(packaging) }
            }.toString()
        )
    }

    private fun File.setZeroDate() {
        // Avoid meaningless Git changes for binary files
        this.setLastModified(0)
    }

    private fun ZipEntry.setZeroDate() {
        // Avoid meaningless Git changes
        creationTime = FileTime.fromMillis(1)
        lastAccessTime = FileTime.fromMillis(1)
        lastModifiedTime = FileTime.fromMillis(1)
    }

    private fun writeEmptyZipFile(file: File) {
        file.outputStream().use { writeEmptyZipFile(it) }
        file.setZeroDate()
    }

    private fun writeEmptyZipFile(zos: OutputStream) {
        // 504B0506 00000000 00000000 00000000 00000000 0000

        zos.write(0x50)
        zos.write(0x4B)
        zos.write(0x05)
        zos.write(0x06)

        repeat(18) { zos.write(0) }
    }
}