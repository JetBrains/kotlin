/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.swc

import kotlinBuildProperties
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.extra

abstract class SwcExtension(
    private val project: Project,
    // Actually `org.jetbrains.kotlin.gradle.targets.js.swc.SwcEnvSpec`, see `SwcBridge` for the reason it is untyped here
    private val swc: Any,
) {
    val swcVersion: String
        get() = project.kotlinBuildProperties.versionsProperty("swc").get()

    // val swcExecutablePath: Provider<String> = swc.executable.also {
    // TODO KT-82969: Remove a line below after bootstrap in favor of the above commented code
    val swcExecutablePath: Provider<String> = SwcBridge.getExecutable(swc).also {
        project.extra["swc.path"] = it
    }

    fun Test.setupSwc() {
//        with(swc) {
//            dependsOn(project.swcSetupTaskProvider)
//        }
        // TODO KT-82969: Remove after bootstrap in favor of the above commented code
        dependsOn(SwcBridge.getSetupTaskProvider(project, swc))

        val swcExecutablePath = swcExecutablePath

        inputs.property("propertyName", "swc.path")
        inputs.property("destinationPath", swcExecutablePath)

        doFirst {
            systemProperty("swc.path", swcExecutablePath.get())
        }
    }
}
