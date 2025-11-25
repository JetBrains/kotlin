/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.swc

import SystemPropertyClasspathProvider
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.newInstance
import org.jetbrains.kotlin.gradle.targets.js.swc.SwcEnvSpec

abstract class SwcExtension(
    private val project: Project,
    private val swc: SwcEnvSpec,
) {
    val swcVersion: String
        get() = project.property("versions.swc") as String

    val swcExecutablePath: Provider<String> = swc.executable.also {
        project.extra["swc.path"] = it
    }

    fun Test.setupSwc() {
        with(swc) {
            dependsOn(project.swcSetupTaskProvider)
        }

        val swcExecutablePath = swcExecutablePath

        inputs.property("propertyName", "swc.path")
        inputs.property("destinationPath", swcExecutablePath)

        doFirst {
            systemProperty("swc.path", swcExecutablePath.get())
        }
    }
}