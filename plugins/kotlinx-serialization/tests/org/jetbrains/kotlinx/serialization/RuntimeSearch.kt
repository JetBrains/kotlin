/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization

import org.jetbrains.kotlin.utils.PathUtil
import org.jetbrains.kotlinx.serialization.compiler.diagnostic.CommonVersionReader
import org.jetbrains.kotlinx.serialization.compiler.diagnostic.VersionReader
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class RuntimeLibraryInClasspathTest {
    companion object {
        val coreLibraryPath = getSerializationLibraryJar("kotlinx.serialization.KSerializer")
        val jsonLibraryPath = getSerializationLibraryJar("kotlinx.serialization.json.Json")

        private fun getSerializationLibraryJar(classToDetect: String): File? = try {
            PathUtil.getResourcePathForClass(Class.forName(classToDetect))
        } catch (e: ClassNotFoundException) {
            null
        }
    }

    @Test
    fun testRuntimeLibraryExists() {
        assertNotNull(
            coreLibraryPath,
            "kotlinx-serialization runtime library is not found. Make sure it is present in test classpath",
        )
    }

    @Test
    fun testRuntimeHasSufficientVersion() {
        val version = CommonVersionReader.getVersionsFromManifest(coreLibraryPath!!)
        assertTrue(version.currentCompilerMatchRequired(), "Runtime version too high")
        assertTrue(version.implementationVersionMatchSupported(), "Runtime version too low")
    }
}
