/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.GradleException
import org.gradle.util.GradleVersion
import org.jetbrains.org.objectweb.asm.ClassWriter
import org.jetbrains.org.objectweb.asm.Opcodes
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.jar.Attributes
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JavaModulePathTest {
    @TempDir
    lateinit var tmp: File

    @Test
    fun modulesAreDetectedLikeJavaCompile() {
        val plain = jar("plain.jar")
        val automatic = jar("automatic.jar", automaticModuleName = "com.example.automatic")
        val explicit = jar("explicit.jar", moduleInfo = "com.example.explicit")
        val multiRelease = jar(
            "multi-release.jar", moduleInfo = "com.example.mr", moduleInfoPath = "META-INF/versions/11/module-info.class",
        )
        val notMultiRelease = jar(
            "not-multi-release.jar", moduleInfo = "com.example.nmr", moduleInfoPath = "META-INF/versions/11/module-info.class",
            multiRelease = false,
        )
        val moduleDirectory = tmp.resolve("module-classes").apply {
            mkdirs()
            resolve("module-info.class").writeBytes(moduleInfo("com.example.directory"))
        }
        val plainDirectory = tmp.resolve("plain-classes").apply { mkdirs() }

        assertEquals(
            listOf(automatic, explicit, multiRelease, moduleDirectory),
            inferJavaModulePath(listOf(plain, automatic, explicit, multiRelease, notMultiRelease, moduleDirectory, plainDirectory)),
        )
    }

    @Test
    fun moduleAfterPlainJarWithSameNameIsKept() {
        val plain = jar("com.example.shared-1.0.jar")
        val explicit = jar("explicit.jar", moduleInfo = "com.example.shared")

        assertEquals(listOf(explicit), inferJavaModulePath(listOf(plain, explicit)))
    }

    @Test
    fun unreadableArchiveFailsFromGradle95() {
        val corrupt = tmp.resolve("corrupt.jar").apply { writeText("this is not a zip archive") }
        val explicit = jar("explicit.jar", moduleInfo = "com.example.explicit")

        assertEquals(listOf(explicit), inferJavaModulePath(listOf(corrupt, explicit), readsWithJarFile = false))
        val failure = assertFailsWith<GradleException> { inferJavaModulePath(listOf(corrupt, explicit), readsWithJarFile = true) }
        assertEquals("Can't read $corrupt to check whether it's a Java module", failure.message)
    }

    @Test
    fun pathArgumentsInFreeArgsAreDetected() {
        val freeArgs = listOf(
            listOf("-Xmodule-path=lib"),
            listOf("-classpath=lib"),
            listOf("-classpath", "lib"),
            listOf("-cp", "lib"),
            listOf("@arguments.txt"),
            listOf("-Xjsr305=strict", "lib"),
        )

        assertEquals(listOf(true, true, true, true, true, false), freeArgs.map(::setsJavaPaths))
    }

    @Test
    fun jarsAreReadWithJarFileFromGradle95Milestone7() {
        for ((version, expected) in listOf(
            "8.14.5" to false, "9.4.1" to false, "9.5.0-milestone-6" to false,
            "9.5.0-milestone-7" to true, "9.5.0-rc-1" to true, "9.5.0" to true, "9.8.2" to true,
        )) {
            assertEquals(expected, readsJarsWithJarFile(GradleVersion.version(version)), version)
        }
    }

    /**
     * Each case's expected results are what Gradle's internal `JavaModuleDetector.isModule` returned for the same entry on
     * Gradle 8.14.5 and 9.4.1 (read with `JarInputStream`) and on 9.5.1 and 9.8.2 (read with `JarFile`). `null` is a failure.
     */
    @Test
    fun modulesAreDetectedLikeGradle() {
        val amn = "Automatic-Module-Name: com.example.amn"
        val mr = "Multi-Release: true"
        val moduleInfo = moduleInfo("com.example.m")
        val cls = "A".toByteArray()
        val isModule = true
        val notModule = false
        val throws: Boolean? = null
        // Results of Gradle's JavaModuleDetector before 9.5 (on 8.14.5 and 9.4.1), then from 9.5 (on 9.5.1 and 9.8.2)
        val cases = listOf(
            Case("plain.jar", notModule, notModule) { zip(it, "META-INF/MANIFEST.MF" to manifest(), "p/A.class" to cls) },
            Case("no-manifest.jar", notModule, notModule) { zip(it, "p/A.class" to cls) },
            Case("amn.jar", isModule, isModule) { zip(it, "META-INF/MANIFEST.MF" to manifest(amn), "p/A.class" to cls) },
            Case("amn-after-meta-inf-dir.jar", isModule, isModule) {
                zip(it, "META-INF/" to ByteArray(0), "META-INF/MANIFEST.MF" to manifest(amn), "p/A.class" to cls)
            },
            Case("amn-late-manifest.jar", notModule, isModule) { zip(it, "p/A.class" to cls, "META-INF/MANIFEST.MF" to manifest(amn)) },
            Case("amn-lowercase-manifest.jar", isModule, isModule) { zip(it, "META-INF/manifest.mf" to manifest(amn), "p/A.class" to cls) },
            Case("amn-lowercase-late-manifest.jar", notModule, isModule) {
                zip(it, "p/A.class" to cls, "META-INF/manifest.mf" to manifest(amn))
            },
            Case("amn-empty-value.jar", isModule, isModule) {
                zip(it, "META-INF/MANIFEST.MF" to manifest("Automatic-Module-Name: "), "p/A.class" to cls)
            },
            Case("dup-manifest-amn-first.jar", isModule, notModule) { duplicateManifests(it, manifest(amn), manifest()) },
            Case("dup-manifest-amn-second.jar", notModule, isModule) { duplicateManifests(it, manifest(), manifest(amn)) },
            Case("root-module-info.jar", isModule, isModule) { zip(it, "module-info.class" to moduleInfo, "p/A.class" to cls) },
            Case("root-module-info-first.jar", isModule, isModule) {
                zip(it, "module-info.class" to moduleInfo, "META-INF/MANIFEST.MF" to manifest())
            },
            Case("module-info-in-subdir.jar", notModule, notModule) {
                zip(it, "META-INF/MANIFEST.MF" to manifest(), "foo/module-info.class" to moduleInfo)
            },
            Case("mr-8.jar", isModule, isModule) { versioned(it, manifest(mr), "8", moduleInfo) },
            Case("mr-9.jar", isModule, isModule) { versioned(it, manifest(mr), "9", moduleInfo) },
            Case("mr-09.jar", isModule, isModule) { versioned(it, manifest(mr), "09", moduleInfo) },
            Case("mr-plus9.jar", notModule, notModule) { versioned(it, manifest(mr), "+9", moduleInfo) },
            Case("mr-11.jar", isModule, isModule) { versioned(it, manifest(mr), "11", moduleInfo) },
            Case("mr-9-and-09.jar", isModule, isModule) {
                zip(
                    it, "META-INF/MANIFEST.MF" to manifest(mr),
                    "META-INF/versions/9/module-info.class" to moduleInfo, "META-INF/versions/09/module-info.class" to moduleInfo,
                )
            },
            Case("mr-module-info-any-char.jar", isModule, isModule) {
                zip(it, "META-INF/MANIFEST.MF" to manifest(mr), "META-INF/versions/9/module-infoXclass" to moduleInfo)
            },
            Case("mr-true-uppercase.jar", isModule, isModule) { versioned(it, manifest("Multi-Release: TRUE"), "9", moduleInfo) },
            Case("mr-yes.jar", notModule, notModule) { versioned(it, manifest("Multi-Release: yes"), "9", moduleInfo) },
            Case("not-mr-versioned.jar", notModule, notModule) { versioned(it, manifest(), "9", moduleInfo) },
            Case("mr-late-manifest.jar", notModule, isModule) {
                zip(it, "META-INF/versions/9/module-info.class" to moduleInfo, "META-INF/MANIFEST.MF" to manifest(mr))
            },
            Case("mr-lowercase-manifest.jar", isModule, isModule) {
                zip(it, "META-INF/manifest.mf" to manifest(mr), "META-INF/versions/9/module-info.class" to moduleInfo)
            },
            Case("prefixed-root-module-info.jar", notModule, isModule) {
                zip(it, "module-info.class" to moduleInfo, "p/A.class" to cls)
                it.writeBytes(ByteArray(128) { '#'.code.toByte() } + it.readBytes())
            },
            Case("no-central-directory-module-info-first.jar", isModule, throws) {
                zip(it, "module-info.class" to moduleInfo, "p/A.class" to cls)
                val bytes = it.readBytes()
                val centralDirectoryHeader = byteArrayOf(0x50, 0x4b, 0x01, 0x02)
                val centralDirectory = (0..bytes.size - 4).first { i -> bytes.copyOfRange(i, i + 4).contentEquals(centralDirectoryHeader) }
                it.writeBytes(bytes.copyOf(centralDirectory))
            },
            Case("empty-zip.jar", notModule, notModule) { zip(it) },
            Case("empty-file.jar", notModule, throws) { it.writeBytes(ByteArray(0)) },
            Case("corrupt.jar", notModule, throws) { it.writeText("this is not a zip archive") },
            Case("amn.zip", notModule, notModule) { zip(it, "META-INF/MANIFEST.MF" to manifest(amn)) },
            Case("AMN-UPPERCASE.JAR", notModule, notModule) { zip(it, "META-INF/MANIFEST.MF" to manifest(amn)) },
            Case("amn-no-extension", notModule, notModule) { zip(it, "META-INF/MANIFEST.MF" to manifest(amn)) },
            Case("dir-mr-versioned", notModule, notModule) {
                it.resolve("META-INF/versions/9").mkdirs()
                it.resolve("META-INF/MANIFEST.MF").writeBytes(manifest(mr))
                it.resolve("META-INF/versions/9/module-info.class").writeBytes(moduleInfo)
            },
            Case("dir-module-info", isModule, isModule) { it.mkdirs(); it.resolve("module-info.class").writeBytes(moduleInfo) },
            Case("dir-module-info-is-directory", isModule, isModule) { it.resolve("module-info.class").mkdirs() },
            Case("dir-amn-manifest", notModule, notModule) {
                it.resolve("META-INF").mkdirs()
                it.resolve("META-INF/MANIFEST.MF").writeBytes(manifest(amn))
            },
            Case("dir-plain", notModule, notModule) { it.resolve("p").mkdirs() },
            Case("missing.jar", notModule, notModule) {},
        )
        for (case in cases) {
            val file = tmp.resolve(case.name).also(case.create)
            for ((readsWithJarFile, expected) in listOf(false to case.beforeGradle95, true to case.fromGradle95)) {
                val actual = runCatching { isGradleJavaModule(file, readsWithJarFile) }.getOrNull()
                assertEquals(expected, actual, "${case.name}, readsWithJarFile = $readsWithJarFile")
            }
        }
    }

    private class Case(val name: String, val beforeGradle95: Boolean?, val fromGradle95: Boolean?, val create: (File) -> Unit)

    private fun manifest(vararg attributes: String): ByteArray =
        (listOf("Manifest-Version: 1.0") + attributes).joinToString("\n", postfix = "\n\n").toByteArray()

    private fun zip(file: File, vararg entries: Pair<String, ByteArray>) {
        ZipOutputStream(file.outputStream()).use { zip ->
            for ((name, bytes) in entries) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
    }

    private fun versioned(file: File, manifest: ByteArray, version: String, moduleInfo: ByteArray) =
        zip(file, "META-INF/MANIFEST.MF" to manifest, "META-INF/versions/$version/module-info.class" to moduleInfo)

    /** ZipOutputStream refuses a second entry of one name, so the second manifest is renamed in place after writing. */
    private fun duplicateManifests(file: File, first: ByteArray, second: ByteArray) {
        zip(file, "META-INF/MANIFEST.MF" to first, "META-INF/MANIFEST.MX" to second)
        val bytes = file.readBytes()
        val placeholder = "META-INF/MANIFEST.MX".toByteArray()
        var renamed = 0
        for (i in 0..bytes.size - placeholder.size) {
            if (bytes.copyOfRange(i, i + placeholder.size).contentEquals(placeholder)) {
                bytes[i + placeholder.size - 1] = 'F'.code.toByte()
                renamed++
            }
        }
        check(renamed == 2) { "Renamed $renamed entries" }
        file.writeBytes(bytes)
    }

    private fun jar(
        name: String,
        automaticModuleName: String? = null,
        moduleInfo: String? = null,
        moduleInfoPath: String = "module-info.class",
        multiRelease: Boolean = true,
        entries: Map<String, ByteArray> = moduleInfo?.let { mapOf(moduleInfoPath to moduleInfo(it)) }.orEmpty(),
    ): File {
        val manifest = Manifest().apply {
            mainAttributes[Attributes.Name.MANIFEST_VERSION] = "1.0"
            if (automaticModuleName != null) mainAttributes.putValue("Automatic-Module-Name", automaticModuleName)
            if (entries.keys.any { it.startsWith("META-INF/versions/") } && multiRelease) mainAttributes.putValue("Multi-Release", "true")
        }
        val file = tmp.resolve(name)
        JarOutputStream(file.outputStream(), manifest).use { jar ->
            for ((path, bytes) in entries) {
                jar.putNextEntry(JarEntry(path))
                jar.write(bytes)
                jar.closeEntry()
            }
        }
        return file
    }

    private fun moduleInfo(moduleName: String): ByteArray =
        ClassWriter(0).apply {
            visit(Opcodes.V9, Opcodes.ACC_MODULE, "module-info", null, null, null)
            visitModule(moduleName, 0, null).visitEnd()
            visitEnd()
        }.toByteArray()
}
