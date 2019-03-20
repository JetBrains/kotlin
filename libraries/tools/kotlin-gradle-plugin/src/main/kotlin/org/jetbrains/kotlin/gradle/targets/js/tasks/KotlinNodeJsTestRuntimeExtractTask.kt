/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

const val kotlinTestNodeJsRunnerJs = "kotlin-test-nodejs-runner.js"

internal val Project.extractedKotlinTestNodeJsRunner: File
    get() = project.buildDir.resolve("kotlin/$kotlinTestNodeJsRunnerJs")

internal val Project.kotlinNodeJsTestRuntimeExtractTask
    get() = project.locateOrRegisterTask<KotlinNodeJsTestRuntimeExtractTask>(
        "kotlinNodeJsTestRuntimeExtract"
    ) { task ->
        task.destination = extractedKotlinTestNodeJsRunner
    }

open class KotlinNodeJsTestRuntimeExtractTask : DefaultTask() {
    @Input
    public var resourceName: String = "/$kotlinTestNodeJsRunnerJs"

    @OutputFile
    @SkipWhenEmpty
    public lateinit var destination: File

    @TaskAction
    fun copyRuntime() {
        val testsRuntime = javaClass.getResourceAsStream(resourceName)
            ?: error("Cannot find `$resourceName` in resources")

        check(destination.parentFile.exists() || destination.parentFile.mkdirs()) {
            "Cannot create directory ${destination.parentFile}"
        }

        Files.copy(
            testsRuntime,
            destination.toPath(),
            StandardCopyOption.REPLACE_EXISTING
        )
    }
}

