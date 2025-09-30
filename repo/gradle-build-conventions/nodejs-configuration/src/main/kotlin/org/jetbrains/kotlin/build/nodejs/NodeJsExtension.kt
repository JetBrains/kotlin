/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.nodejs

import SystemPropertyClasspathProvider
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.newInstance
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension

abstract class NodeJsExtension(
    private val nodeJsRoot: NodeJsRootExtension
) {
    @Suppress("DEPRECATION", "DEPRECATION_ERROR")
    fun Test.setupNodeJs(version: String) {
        dependsOn(nodeJsRoot.nodeJsSetupTaskProvider)
        nodeJsRoot.version = version
        val nodeJsExecutablePath = project.provider {
            nodeJsRoot.requireConfigured().nodeExecutable
        }
        jvmArgumentProviders += this.project.objects.newInstance<SystemPropertyClasspathProvider>().apply {
            classpath.from(nodeJsExecutablePath)
            property.set("javascript.engine.path.NodeJs")
        }
    }
}