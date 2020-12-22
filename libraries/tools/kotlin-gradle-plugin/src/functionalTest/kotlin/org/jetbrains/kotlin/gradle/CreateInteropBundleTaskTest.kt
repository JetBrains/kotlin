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
import kotlin.test.Test
import kotlin.test.assertEquals

class CreateInteropBundleTaskTest {

    @Test
    fun createSimpleInteropBundle() {
        val project = ProjectBuilder.builder().build()

        project.buildDir.mkdirs()
        val linuxX64Klib = project.buildDir.resolve("linux.klib")
        val macosX64Klib = project.buildDir.resolve("macos.klib")
        linuxX64Klib.writeText("linux stub")
        macosX64Klib.writeText("macos stub")

        project.configurations.create(KonanTarget.LINUX_X64.name)
        project.configurations.create(KonanTarget.MACOS_X64.name)

        project.dependencies.run {
            add(KonanTarget.LINUX_X64.name, project.files(linuxX64Klib))
            add(KonanTarget.MACOS_X64.name, project.files(macosX64Klib))
        }

        val outputDirectory = project.buildDir.resolve("testInteropBundle")

        val task = project.tasks.create("createInteropBundle", CreateInteropBundleTask::class.java)
        task.outputDirectory.set(outputDirectory)
        task.createInteropBundle()

        val interopBundle = InteropBundle(outputDirectory)
        assertEquals("macos stub", interopBundle.listLibraries(KonanTarget.MACOS_X64).single().readText())
        assertEquals("linux stub", interopBundle.listLibraries(KonanTarget.LINUX_X64).single().readText())
        assertEquals(2, interopBundle.listLibraries().size, "Expected only two libraries")
    }
}
