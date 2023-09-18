/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.targets.js.d8.D8RootExtension
import org.jetbrains.kotlin.gradle.targets.js.d8.D8RootPlugin
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin

private object V8Utils {
    lateinit var d8Plugin: D8RootExtension

    fun useD8Plugin(project: Project) {
        d8Plugin = D8RootPlugin.apply(project.rootProject)
        d8Plugin.version = project.v8Version
    }
}

fun Project.useD8Plugin() {
    V8Utils.useD8Plugin(this)
}

fun Test.setupV8() {
    dependsOn(V8Utils.d8Plugin.setupTaskProvider)
    val v8ExecutablePath = project.provider {
        V8Utils.d8Plugin.requireConfigured().executablePath.absolutePath
    }
    doFirst {
        systemProperty("javascript.engine.path.V8", v8ExecutablePath.get())
    }
}


private object NodeJsUtils {
    lateinit var nodeJsPlugin: NodeJsRootExtension

    fun useNodeJsPlugin(project: Project) {
        nodeJsPlugin = NodeJsRootPlugin.apply(project.rootProject)
    }
}

fun Project.useNodeJsPlugin() {
    NodeJsUtils.useNodeJsPlugin(this)
}

fun Test.setupNodeJs() {
    dependsOn(NodeJsUtils.nodeJsPlugin.nodeJsSetupTaskProvider)
    val nodeJsExecutablePath = project.provider {
        NodeJsUtils.nodeJsPlugin.requireConfigured().nodeExecutable
    }
    doFirst {
        systemProperty("javascript.engine.path.NodeJs", nodeJsExecutablePath.get())
    }
}

