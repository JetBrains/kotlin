/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.nodejs

import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension

abstract class NodeJsExtension(
    private val nodeJsRoot: NodeJsRootExtension
) {
    @Suppress("DEPRECATION", "DEPRECATION_ERROR")
    fun Test.setupNodeJs() {
        dependsOn(nodeJsRoot.nodeJsSetupTaskProvider)
        val nodeJsExecutablePath = project.provider {
            nodeJsRoot.requireConfigured().nodeExecutable
        }
        doFirst {
            systemProperty("javascript.engine.path.NodeJs", nodeJsExecutablePath.get())
        }
    }
}