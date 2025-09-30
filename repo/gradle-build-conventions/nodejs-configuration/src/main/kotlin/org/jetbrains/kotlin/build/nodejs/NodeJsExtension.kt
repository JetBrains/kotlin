/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.nodejs

import SystemPropertyClasspathProvider
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.newInstance
import org.jetbrains.kotlin.gradle.targets.web.nodejs.BaseNodeJsEnvSpec

abstract class NodeJsExtension(
    private val project: Project,
    private val nodeJsEnvSpec: BaseNodeJsEnvSpec,
) {

    val nodeJsExecutablePath: Provider<String> = nodeJsEnvSpec.executable.also {
        project.extra["javascript.engine.path.NodeJs"] = it
    }

    fun Test.setupNodeJs(version: String) {
        with(nodeJsEnvSpec) {
            dependsOn(project.nodeJsSetupTaskProvider)
        }
        nodeJsEnvSpec.version.set(version)
        jvmArgumentProviders += this.project.objects.newInstance<SystemPropertyClasspathProvider>().apply {
            classpath.from(nodeJsExecutablePath)
            property.set("javascript.engine.path.NodeJs")
        }
    }
}