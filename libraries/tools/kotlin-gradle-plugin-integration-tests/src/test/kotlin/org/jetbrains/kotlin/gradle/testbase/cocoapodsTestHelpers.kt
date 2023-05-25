/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.jetbrains.kotlin.gradle.util.replaceText
import java.nio.file.Path
import kotlin.io.path.appendText

val String.normalizeCocoapadsFrameworkName: String
    get() = replace('-', '_')

fun TestProject.useCustomCocoapodsFrameworkName(
    subprojectName: String,
    frameworkName: String,
    iosAppLocation: String? = null,
) {
    // Change the name at the Gradle side.
    subProject(subprojectName)
        .buildGradleKts
        .addFrameworkBlock("baseName = \"$frameworkName\"")

    // Change swift sources import if needed.
    if (iosAppLocation != null) {
        projectPath
            .resolve(iosAppLocation)
            .resolve("ios-app/ViewController.swift")
            .replaceText(
                "import ${subprojectName.normalizeCocoapadsFrameworkName}",
                "import $frameworkName"
            )
    }
}

fun Path.addCocoapodsBlock(str: String) = addKotlinBlock(str.wrapIntoBlock("cocoapods"))

private fun Path.addKotlinBlock(str: String) = appendLine(str.wrapIntoBlock("kotlin"))

private fun Path.addFrameworkBlock(str: String) = addCocoapodsBlock(str.wrapIntoBlock("framework"))

private fun Path.appendLine(s: String) = appendText("\n$s")