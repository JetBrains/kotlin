/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("DEPRECATION")

import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin


private object NodeJsUtils {
    lateinit var nodeJsPlugin: NodeJsRootExtension

    fun useNodeJsPlugin(project: Project) {
        nodeJsPlugin = NodeJsRootPlugin.apply(project.rootProject)
    }
}

fun Project.useNodeJsPlugin() {
    NodeJsUtils.useNodeJsPlugin(this)
}

@Suppress("DEPRECATION")
fun Test.setupNodeJs() {
    dependsOn(NodeJsUtils.nodeJsPlugin.nodeJsSetupTaskProvider)
    val nodeJsExecutablePath = project.provider {
        NodeJsUtils.nodeJsPlugin.requireConfigured().nodeExecutable
    }
    doFirst {
        systemProperty("javascript.engine.path.NodeJs", nodeJsExecutablePath.get())
    }
}

