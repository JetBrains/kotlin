/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.jetbrains.kotlin.gradle.util.modify
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists

internal fun Path.applyAndroidTestFixes() {
    // Path relative to the current gradle module project dir
    val keystoreFile = Paths.get("src/test/resources/common/debug.keystore")
    assert(keystoreFile.exists()) {
        "Common 'debug.keystore' file does not exists in ${keystoreFile.toAbsolutePath()} location!"
    }
    resolve("gradle.properties").append(
        """
        |test.fixes.android.debugKeystore=${keystoreFile.toAbsolutePath().toString().normalizePath()}
        |
        """.trimMargin()
    )

    val pathFile = toFile()

    pathFile.walkTopDown()
        .filter { it.name == "build.gradle" || it.name == "build.gradle.kts" }
        .forEach { file ->
            when (file.name) {
                "build.gradle" -> file.updateBuildGradle()
                "build.gradle.kts" -> file.updateBuildGradleKts()
            }
        }
}

private fun File.updateBuildGradle() {
    modify {
        if (it.contains("plugins {")) {
            """
            |${it.substringBefore("plugins {")}
            |plugins {
            |    id "org.jetbrains.kotlin.test.fixes.android"
            |${it.substringAfter("plugins {")}
            """.trimMargin()
        } else if (it.contains("apply plugin:")) {
            it.modifyBuildScript().run {
                """
                |${substringBefore("apply plugin:")}
                |apply plugin: 'org.jetbrains.kotlin.test.fixes.android'
                |apply plugin:${substringAfter("apply plugin:")}
                """.trimMargin()
            }
        } else {
            it.modifyBuildScript()
        }
    }
}

private fun String.modifyBuildScript(isKts: Boolean = false): String =
    if (contains("buildscript {") &&
        contains("classpath")
    ) {
        val kotlinVersionStr = if (isKts) "${'$'}{property(\"test_fixes_version\")}" else "${'$'}test_fixes_version"
        """
        |${substringBefore("classpath")}
        |classpath("org.jetbrains.kotlin:android-test-fixes:$kotlinVersionStr")
        |classpath${substringAfter("classpath")}
        """.trimMargin()
    } else {
        this
    }


private fun File.updateBuildGradleKts() {
    modify {
        if (it.contains("plugins {")) {
            """
            |${it.substringBefore("plugins {")}
            |plugins {
            |    id("org.jetbrains.kotlin.test.fixes.android")
            |${it.substringAfter("plugins {")}
            """.trimMargin()
        } else if (it.contains("apply(plugin")) {
            it.modifyBuildScript(true).run {
                """
                |${substringBefore("apply(plugin")}
                |apply(plugin = "org.jetbrains.kotlin.test.fixes.android")
                |apply(plugin${substringAfter("apply(plugin")}
                """.trimMargin()
            }
        } else {
            it.modifyBuildScript(true)
        }
    }
}