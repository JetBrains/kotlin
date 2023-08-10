/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.jetbrains.kotlin.gradle.util.modify
import java.io.File
import java.nio.file.Path

internal fun Path.applyPlugin(pluginId: String, artifactId: String, artifactVersionProperty: String) {
    val pathFile = toFile()

    val groovyBuildScripts = setOf("build.gradle", "build-js.gradle")
    val kotlinBuildScripts = setOf("build.gradle.kts", "build-js.gradle.kts", "build.gradle.kts.alternative")
    pathFile.walkTopDown()
        .filter { it.name in groovyBuildScripts || it.name in kotlinBuildScripts }
        .forEach { file ->
            when (file.name) {
                in groovyBuildScripts -> file.updateBuildGradle(pluginId, artifactId, artifactVersionProperty)
                in kotlinBuildScripts -> file.updateBuildGradleKts(pluginId, artifactId, artifactVersionProperty)
            }
        }
}

private fun File.updateBuildGradle(pluginId: String, artifactId: String, artifactVersionProperty: String) {
    modify {
        if (it.contains("plugins {")) {
            """
            |${it.substringBefore("plugins {")}
            |plugins {
            |    id "$pluginId"
            |${it.substringAfter("plugins {")}
            """.trimMargin()
        } else if (it.contains("apply plugin:")) {
            it.modifyBuildScript(artifactId, artifactVersionProperty).run {
                """
                |${substringBefore("apply plugin:")}
                |apply plugin: '$pluginId'
                |apply plugin:${substringAfter("apply plugin:")}
                """.trimMargin()
            }
        } else {
            it.modifyBuildScript(artifactId, artifactVersionProperty)
        }
    }
}

private fun String.modifyBuildScript(artifactId: String, artifactVersionProperty: String, isKts: Boolean = false): String =
    if (contains("buildscript {") &&
        contains("classpath")
    ) {
        val kotlinVersionStr = if (isKts) "${'$'}{property(\"$artifactVersionProperty\")}" else "${'$'}$artifactVersionProperty"
        """
        |${substringBefore("classpath")}
        |classpath("$artifactId:$kotlinVersionStr")
        |classpath${substringAfter("classpath")}
        """.trimMargin()
    } else {
        this
    }


private fun File.updateBuildGradleKts(pluginId: String, artifactId: String, artifactVersionProperty: String) {
    modify {
        if (it.contains("plugins {")) {
            """
            |${it.substringBefore("plugins {")}
            |plugins {
            |    id("$pluginId")
            |${it.substringAfter("plugins {")}
            """.trimMargin()
        } else if (it.contains("apply(plugin")) {
            it.modifyBuildScript(artifactId, artifactVersionProperty, true).run {
                """
                |${substringBefore("apply(plugin")}
                |apply(plugin = "$pluginId")
                |apply(plugin${substringAfter("apply(plugin")}
                """.trimMargin()
            }
        } else {
            it.modifyBuildScript(artifactId, artifactVersionProperty, true)
        }
    }
}