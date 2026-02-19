/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.util.MultiplatformExtensionTest
import org.jetbrains.kotlin.gradle.plugin.mpp.DefaultCInteropSettings
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import org.jetbrains.kotlin.gradle.utils.getFile
import kotlin.test.Test
import kotlin.test.assertEquals

class CInteropTaskTest : MultiplatformExtensionTest() {

    @Test
    fun `cinterop properties can be changed after creating`() {
        fun DefaultCInteropSettings.setDummyValues(prefix: String) {
            dependencyFiles = project.files("${prefix}-dependencyFile")
            defFile("${prefix}-defFile")
            packageName = "${prefix}-packageName"
            compilerOpts += "${prefix}-compilerOpts"
            linkerOpts += "${prefix}-linkerOpts"
            extraOpts = listOf("${prefix}-extraOpts")
            header("${prefix}-header.h")

            includeDirs {
                allHeaders("${prefix}-allHeaders")
                headerFilterOnly("${prefix}-headerFilterOnly")
            }
        }

        val linuxX64 = kotlin.linuxX64().apply {
            compilations.getByName("main").cinterops.create("dummy") {
                it.setDummyValues(prefix = "default")
            }
        }

        // Get task to make sure it is configured
        val cinteropTask = project.tasks.getByName("cinteropDummyLinuxX64") as CInteropProcess

        // Change cinterop properties
        val cinterop = linuxX64.compilations.getByName("main").cinterops.getByName("dummy")
        cinterop.setDummyValues("updated")

        project.evaluate()

        assertEquals("updated-dependencyFile", cinteropTask.libraries.files.single().name)
        assertEquals("updated-defFile", cinteropTask.definitionFile.getFile().name)
        assertEquals("updated-packageName", cinteropTask.packageName)
        assertEquals(listOf("default-compilerOpts", "updated-compilerOpts"), cinteropTask.compilerOpts)
        assertEquals(listOf("default-linkerOpts", "updated-linkerOpts"), cinteropTask.linkerOpts)
        assertEquals(listOf("updated-extraOpts"), cinteropTask.extraOpts)
        assertEquals(listOf("default-header.h", "updated-header.h"), cinteropTask.headers.files.map { it.name })
        assertEquals(listOf("default-allHeaders", "updated-allHeaders"), cinteropTask.allHeadersDirs.map { it.name })
        assertEquals(listOf("default-headerFilterOnly", "updated-headerFilterOnly"), cinteropTask.headerFilterDirs.map { it.name })
    }
}