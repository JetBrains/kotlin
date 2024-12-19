/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.targets.web.nodejs.BaseNodeJsEnvSpec

/**
 * Spec for Node.js - common JS and Wasm runtime.
 */
abstract class NodeJsEnvSpec : BaseNodeJsEnvSpec() {

    override val Project.nodeJsSetupTaskProvider: TaskProvider<out NodeJsSetupTask>
        get() = project.tasks.withType(NodeJsSetupTask::class.java)
            .named(NodeJsSetupTask.NAME)

    companion object {
        const val EXTENSION_NAME: String = "kotlinNodeJsSpec"
    }
}
