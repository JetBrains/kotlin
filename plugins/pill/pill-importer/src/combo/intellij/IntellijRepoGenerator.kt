/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.pill.combo.intellij

import org.jetbrains.kotlin.pill.xml
import java.io.File
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class IntellijRepoGenerator(kotlinProjectDir: File) : IntellijComboGeneratorBase(kotlinProjectDir) {
    companion object {
        private const val GROUP_ID = "com.jetbrains.intellij.idea"
        private const val VERSION = "202.0"
    }

    fun generate() {
        val repoPath = System.getProperty("pill.combo.intellij.repo.path", null) ?: error("Repository path is not specified")
        val repoDir = File(repoPath)
        check(repoDir.parentFile.isDirectory) { "Repository path parent does not exist" }

        if (!repoDir.deleteRecursively()) {
            error("Can not delete existing repository $repoDir")
        }

        val groupDir = File(repoDir, GROUP_ID.replace('.', '/'))

        fun getNameBase(artifactId: String) = "$artifactId/$VERSION/$artifactId-$VERSION"

        for (artifactId in substitutions.artifacts) {
            val substitutionsForArtifact = substitutions.getForArtifact(artifactId) ?: continue
            val nameBase = getNameBase(artifactId)
            val artifactFile = File(groupDir, "$nameBase.zip")
            artifactFile.parentFile.mkdirs()

            artifactFile.outputStream().use { fos ->
                ZipOutputStream(fos).use { zos ->
                    for (path in substitutionsForArtifact.keys) {
                        val entry = ZipEntry(path)
                        zos.putNextEntry(entry)

                        if (path.toLowerCase().endsWith(".jar")) {
                            writeEmptyZipFile(zos)
                        }

                        zos.closeEntry()
                    }
                }
            }

            writePom(File(groupDir, "$nameBase.pom"), artifactId, "zip")

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
            writePom(File(groupDir, "$nameBase.pom"), artifactId, "jar")
        }
    }

    private fun writePom(file: File, artifactId: String, packaging: String) {
        file.writeText(
            xml("project") {
                xml("modelVersion") { raw("4.0.0") }
                xml("groupId") { raw(GROUP_ID) }
                xml("artifactId") { raw(artifactId) }
                xml("version") { raw(VERSION) }
                xml("packaging") { raw(packaging) }
            }.toString()
        )
    }

    private fun writeEmptyZipFile(file: File) {
        file.outputStream().use { writeEmptyZipFile(it) }
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