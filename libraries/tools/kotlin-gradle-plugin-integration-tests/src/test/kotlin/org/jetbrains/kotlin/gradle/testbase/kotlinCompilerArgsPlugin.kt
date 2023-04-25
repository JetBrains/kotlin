/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.jetbrains.kotlin.gradle.util.modify
import java.io.File
import java.nio.file.Path

fun Path.applyKotlinCompilerArgsPlugin() {
    toFile().walkTopDown()
        .filter { it.name in listOf("build.gradle", "build.gradle.kts", "settings.gradle", "settings.gradle.kts") }
        .forEach { file ->
            when (file.name) {
                "build.gradle" -> file.updateBuildGradle()
                "build.gradle.kts" -> file.updateBuildKtsGradle()
                "settings.gradle.kts" -> file.updateSettingsKtsGradle()
                "settings.gradle" -> file.updateSettingsGradle()
            }
        }
}

private fun File.updateSettingsKtsGradle() {
    modify {
        if (it.contains("pluginManagement {")) {
                it.replaceFirst(
                    "plugins {", "plugins {\n" +
                            "id(\"org.jetbrains.kotlin.test.kotlin-compiler-args-properties\") version test_fixes_version"
                )
        } else it
    }
}

private fun File.updateSettingsGradle() {
    modify {
        if (it.contains("pluginManagement {")) {
            it.replaceFirst("plugins {", "plugins {\n"+
                    "id \"org.jetbrains.kotlin.test.kotlin-compiler-args-properties\" version \"${'$'}test_fixes_version\"")
        } else it
    }
}

private fun File.updateBuildKtsGradle() {
    modify {
        it.replace(
            "plugins {",
            "plugins {\nid(\"org.jetbrains.kotlin.test.kotlin-compiler-args-properties\")"
        )
    }
}

private fun File.updateBuildGradle() {
    modify {
        if (it.contains("buildscript {")) {
            it.replaceFirst(
                "dependencies {", "dependencies {\n" +
                        "classpath \"org.jetbrains.kotlin:kotlin-compiler-args-properties:${'$'}test_fixes_version\""
            )
        } else {
            it.replace(
                "plugins {",
                "plugins {\nid \"org.jetbrains.kotlin.test.kotlin-compiler-args-properties\""
            )
        }
    }
}
