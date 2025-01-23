/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.nodejs

import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.targets.web.HasPlatformDisambiguator
import org.jetbrains.kotlin.gradle.targets.web.nodejs.BaseNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsSetupTask

/**
 * Spec for Node.js - common JS and Wasm runtime.
 */
abstract class WasmNodeJsEnvSpec : BaseNodeJsEnvSpec() {

    override val Project.nodeJsSetupTaskProvider: TaskProvider<out NodeJsSetupTask>
        get() = project.tasks.withType(NodeJsSetupTask::class.java)
            .named(extensionName(NodeJsSetupTask.Companion.NAME))

    companion object : HasPlatformDisambiguator by WasmPlatformDisambiguator {
        val EXTENSION_NAME: String
            get() = extensionName("kotlinNodeJsSpec")
    }
}