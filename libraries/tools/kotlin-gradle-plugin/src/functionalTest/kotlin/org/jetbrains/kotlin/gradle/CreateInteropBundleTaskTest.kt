/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
/* Associate compilations are not yet supported by the IDE. KT-34102 */
@file:Suppress("invisible_reference", "invisible_member", "FunctionName", "DuplicatedCode")

package org.jetbrains.kotlin.gradle

import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.ib.CreateInteropBundleTask
import org.jetbrains.kotlin.gradle.ib.InteropBundle
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals

class CreateInteropBundleTaskTest {

    @Test
    fun createSimpleInteropBundle() {
        val project = ProjectBuilder.builder().build()

        project.buildDir.mkdirs()

        val linuxKlib = project.buildDir.resolve("linux.klib")
        ZipOutputStream(linuxKlib.outputStream()).use { stream ->
            val entry = ZipEntry("linux-content-stub")
            stream.putNextEntry(entry)
            stream.write("linux stub".toByteArray())
        }

        val macosKlib = project.buildDir.resolve("macos.klib")
        ZipOutputStream(macosKlib.outputStream()).use { stream ->
            val entry = ZipEntry("macos-content-stub")
            stream.putNextEntry(entry)
            stream.write("macos stub".toByteArray())
        }

        project.configurations.create(KonanTarget.LINUX_X64.name)
        project.configurations.create(KonanTarget.MACOS_X64.name)

        project.dependencies.run {
            add(KonanTarget.LINUX_X64.name, project.files(linuxKlib))
            add(KonanTarget.MACOS_X64.name, project.files(macosKlib))
        }

        val outputDirectory = project.buildDir.resolve("testInteropBundle")

        val task = project.tasks.create("createInteropBundle", CreateInteropBundleTask::class.java)
        task.outputDirectory.set(outputDirectory)
        task.createInteropBundle()

        val interopBundle = InteropBundle(outputDirectory)
        assertEquals("macos stub", interopBundle.resolve(KonanTarget.MACOS_X64).resolve("macos-content-stub").readText())
        assertEquals("linux stub", interopBundle.resolve(KonanTarget.LINUX_X64).resolve("linux-content-stub").readText())
        assertEquals(2, interopBundle.listLibraries().size, "Expected only two libraries")
        assertEquals(1, interopBundle.resolve(KonanTarget.MACOS_X64).listFiles()?.size, "Expected only one stub entry")
        assertEquals(1, interopBundle.resolve(KonanTarget.LINUX_X64).listFiles()?.size, "Expected only one stub entry")
    }
}
