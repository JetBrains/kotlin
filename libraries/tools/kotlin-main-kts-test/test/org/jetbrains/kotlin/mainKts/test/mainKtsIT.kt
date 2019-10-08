/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.mainKts.test

import junit.framework.Assert
import org.jetbrains.kotlin.scripting.compiler.plugin.runWithK2JVMCompiler
import org.jetbrains.kotlin.scripting.compiler.plugin.runWithKotlinc
import org.junit.Test
import java.io.File

class MainKtsIT {

    @Test
    fun testResolveJunit() {
        runWithKotlincAndMainKts("$TEST_DATA_ROOT/hello-resolve-junit.main.kts", listOf("Hello, World!"))
    }

    @Test
    fun testImport() {
        val mainKtsJar = File("dist/kotlinc/lib/kotlin-main-kts.jar")
        Assert.assertTrue("kotlin-main-kts.jar not found, run dist task: ${mainKtsJar.absolutePath}", mainKtsJar.exists())

        runWithK2JVMCompiler(
            "$TEST_DATA_ROOT/import-test.main.kts",
            listOf("Hi from common", "Hi from middle", "sharedVar == 5"),
            classpath = listOf(mainKtsJar)
        )
    }

    @Test
    fun testThreadContextClassLoader() {
        runWithKotlincAndMainKts("$TEST_DATA_ROOT/context-classloader.main.kts", listOf("MainKtsConfigurator"))
    }
}

fun runWithKotlincAndMainKts(
    scriptPath: String,
    expectedOutPatterns: List<String> = emptyList(),
    expectedExitCode: Int = 0
) = runWithKotlinc(
    scriptPath, expectedOutPatterns, expectedExitCode,
    classpath = listOf(
        File("dist/kotlinc/lib/kotlin-main-kts.jar").also {
            Assert.assertTrue("kotlin-main-kts.jar not found, run dist task: ${it.absolutePath}", it.exists())
        }
    )
)

