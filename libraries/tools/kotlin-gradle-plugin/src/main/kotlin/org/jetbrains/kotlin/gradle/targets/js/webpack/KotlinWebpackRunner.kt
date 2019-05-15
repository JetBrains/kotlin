/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress(
    "JSAnnotator",
    "NodeJsCodingAssistanceForCoreModules",
    "BadExpressionStatementJS",
    "JSUnresolvedFunction"
)

package org.jetbrains.kotlin.gradle.targets.js.webpack

import org.gradle.api.Project
import org.gradle.process.ExecResult
import org.gradle.process.internal.ExecHandle
import org.gradle.process.internal.ExecHandleFactory
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmResolver
import java.io.File

internal class KotlinWebpackRunner(
    val project: Project,
    val configFile: File,
    val execHandleFactory: ExecHandleFactory,
    val bin: String,
    val configWriter: KotlinWebpackConfigWriter
) {
    fun execute(): ExecResult {
        val exec = start()
        val result = exec.waitForFinish()
        check(result.exitValue == 0) {
            "Webpack exited with non zero exit code (${result.exitValue})"
        }
        return result
    }

    fun start(): ExecHandle {
        check(configWriter.entry?.isFile == true) {
            "${this}: Entry file not existed \"${configWriter.entry}\""
        }

        NpmResolver.resolve(project)

        val npmProjectLayout = NpmProject[project]

        configWriter.save(configFile)

        val execFactory = execHandleFactory.newExec()

        val args = mutableListOf<String>("--config", configFile.absolutePath)
        if (configWriter.showProgress) {
            args.add("--progress")
        }

        npmProjectLayout.useTool(execFactory, ".bin/$bin", *args.toTypedArray())
        val exec = execFactory.build()
        exec.start()
        return exec
    }
}