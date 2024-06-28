/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp.resources

import org.gradle.testkit.runner.BuildResult
import org.jetbrains.kotlin.gradle.testbase.BuildOptions
import org.jetbrains.kotlin.gradle.testbase.TestProject
import org.jetbrains.kotlin.gradle.testbase.build
import org.jetbrains.kotlin.gradle.testbase.buildAndFail
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.zip.ZipFile

fun TestProject.buildWithAGPVersion(
    vararg buildArguments: String,
    androidVersion: String,
    defaultBuildOptions: BuildOptions,
    assertions: BuildResult.() -> Unit = {},
) {
    build(
        *buildArguments,
        buildOptions = defaultBuildOptions.copy(androidVersion = androidVersion),
        assertions = assertions,
    )
}

fun TestProject.buildAndFailWithAGPVersion(
    vararg buildArguments: String,
    androidVersion: String,
    defaultBuildOptions: BuildOptions,
    assertions: BuildResult.() -> Unit = {},
) {
    buildAndFail(
        *buildArguments,
        buildOptions = defaultBuildOptions.copy(androidVersion = androidVersion),
        assertions = assertions,
    )
}

fun unzip(
    inputZip: Path,
    outputDir: Path,
    filesStartingWith: String,
) {
    ZipFile(inputZip.toFile()).use {
        it.entries().asSequence().filter { it.name.startsWith(filesStartingWith) && !it.isDirectory }.forEach { entry ->
            val outputFile = outputDir.resolve(Paths.get(entry.name))
            if (!outputFile.parent.toFile().exists())
                Files.createDirectories(outputFile.parent)

            it.getInputStream(entry).use { input ->
                Files.copy(input, outputFile, StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }
}