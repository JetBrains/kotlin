/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.nodejs

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.extra
import org.jetbrains.kotlin.gradle.targets.web.nodejs.BaseNodeJsEnvSpec

abstract class NodeJsExtension(
    private val project: Project,
    private val nodeJsEnvSpec: BaseNodeJsEnvSpec,
    private val nodejsPropertyName: String,
    private val nodejsVersionName: String,
) {
    val nodeJsVersion: String
        get() = project.property(nodejsVersionName) as String

    val nodeJsExecutablePath: Provider<String> = nodeJsEnvSpec.executable.also {
        project.extra[nodejsPropertyName] = it
    }

    fun Test.setupNodeJs(version: String) {
        with(nodeJsEnvSpec) {
            dependsOn(project.nodeJsSetupTaskProvider)
        }

        nodeJsEnvSpec.version.set(version)

        val nodejsPropertyName = nodejsPropertyName
        val nodeJsExecutablePath = nodeJsExecutablePath

        inputs.property("propertyName", nodejsPropertyName)
        inputs.property("destinationPath", nodeJsExecutablePath)

        doFirst {
            systemProperty(nodejsPropertyName, nodeJsExecutablePath.get())
        }
    }
}
