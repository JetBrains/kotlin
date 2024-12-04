/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.base.incremental

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class IncrementalProcessorDiscoveryTest {

    private val markerFileContent = """
                    Input1Processor1,AGGREGATING
                    Input1Processor2,ISOLATING
                    Input1Processor3,DYNAMIC
                    Input1Processor4,UNKNOWN
                    Input1Processor5 this is malformed input
            """.trimIndent()

    @Test
    fun locateInJars(@TempDir tmp: File) {
        val inputJar = tmp.resolve("inputJar.jar")
        ZipOutputStream(inputJar.outputStream()).use {
            it.putNextEntry(ZipEntry("META-INF/gradle/incremental.annotation.processors"))
            it.write(markerFileContent.toByteArray())
            it.closeEntry()
        }

        val info = getIncrementalProcessorsFromClasspath(
            setOf("Input1Processor4", "Input1Processor3", "Input1Processor2", "Input1Processor1"),
            listOf(inputJar)
        )

        assertEquals(
            mapOf(
                "Input1Processor1" to DeclaredProcType.AGGREGATING,
                "Input1Processor2" to DeclaredProcType.ISOLATING,
                "Input1Processor3" to DeclaredProcType.DYNAMIC
            ),
            info
        )
    }

    @Test
    fun locateInDir(@TempDir tmp: File) {
        val inputDir = tmp.resolve("inputDir")
        inputDir.resolve("META-INF/gradle/incremental.annotation.processors").let {
            it.parentFile.mkdirs()
            it.writeText(markerFileContent)
        }

        val info = getIncrementalProcessorsFromClasspath(
            setOf("Input1Processor4", "Input1Processor3", "Input1Processor2", "Input1Processor1"),
            listOf(inputDir)
        )

        assertEquals(
            mapOf(
                "Input1Processor1" to DeclaredProcType.AGGREGATING,
                "Input1Processor2" to DeclaredProcType.ISOLATING,
                "Input1Processor3" to DeclaredProcType.DYNAMIC
            ),
            info
        )
    }

    @Test
    fun locateInJarsAndDirs(@TempDir tmp: File) {
        val inputJar = tmp.resolve("inputJar.jar")
        ZipOutputStream(inputJar.outputStream()).use {
            it.putNextEntry(ZipEntry("META-INF/gradle/incremental.annotation.processors"))
            it.write("InputJarProcessor,ISOLATING".toByteArray())
            it.closeEntry()
        }

        val inputDir = tmp.resolve("inputDir")
        inputDir.resolve("META-INF/gradle/incremental.annotation.processors").let {
            it.parentFile.mkdirs()
            it.writeText("InputDirProcessor,DYNAMIC")
        }

        val info = getIncrementalProcessorsFromClasspath(
            setOf("InputJarNonIncrementalProcessor", "InputJarProcessor", "InputDirNonIncrementalProcessor", "InputDirProcessor"),
            listOf(inputJar, inputDir)
        )
        assertEquals(
            mapOf(
                "InputJarProcessor" to DeclaredProcType.ISOLATING,
                "InputDirProcessor" to DeclaredProcType.DYNAMIC
            ),
            info
        )
    }
}
