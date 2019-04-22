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
    val config: KotlinWebpackConfig
) {
    fun execute() {
        val exec = start()
        exec.waitForFinish()
    }

    fun start(): ExecHandle {
        check(config.entry.isFile) {
            "${this}: Entry file not existed \"${config.entry}\""
        }

        NpmResolver.resolve(project)

        val npmProjectLayout = NpmProject[project]

        config.save(configFile)

        val execFactory = execHandleFactory.newExec()
        val args = mutableListOf<String>("--config", configFile.absolutePath)
        if (config.showProgress) {
            args.add("--progress")
        }

        npmProjectLayout.useTool(execFactory, ".bin/$bin")
        val exec = execFactory.build()
        exec.start()
        return exec
    }
}