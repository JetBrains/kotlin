/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("DEPRECATION")

import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.the
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.d8.D8EnvSpec
import org.jetbrains.kotlin.gradle.targets.js.d8.D8Plugin
import org.jetbrains.kotlin.gradle.targets.js.d8.D8RootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin

@OptIn(ExperimentalWasmDsl::class)
private object V8Utils {
    lateinit var d8Plugin: D8EnvSpec
    lateinit var d8Root: D8RootExtension

    fun useD8Plugin(project: Project) {
        project.plugins.apply(D8Plugin::class.java)
        d8Plugin = project.the<D8EnvSpec>()
        project.rootProject.plugins.apply(D8Plugin::class.java)
        d8Root = project.rootProject.the<D8RootExtension>()
        d8Plugin.version.set(project.v8Version)
        @Suppress("DEPRECATION")
        d8Root.version = project.v8Version
    }
}

fun Project.useD8Plugin() {
    V8Utils.useD8Plugin(this)
}

@OptIn(ExperimentalWasmDsl::class)
fun Test.setupV8() {
    with(V8Utils.d8Plugin) {
        dependsOn(project.d8SetupTaskProvider)
    }
    dependsOn(V8Utils.d8Root.setupTaskProvider)
    val v8ExecutablePath = project.provider {
        V8Utils.d8Root.requireConfigured().executablePath.absolutePath
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

