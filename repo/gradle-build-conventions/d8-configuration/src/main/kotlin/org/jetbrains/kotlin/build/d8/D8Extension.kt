/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalWasmDsl::class)

package org.jetbrains.kotlin.build.d8

import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.d8.D8EnvSpec
import org.jetbrains.kotlin.gradle.targets.js.d8.D8RootExtension

abstract class D8Extension(
    private val d8envSpec: D8EnvSpec,
    private val d8Root: D8RootExtension
) {
    val Project.v8Version: String get() = property("versions.v8") as String

    fun Test.setupV8() {
        with(d8envSpec) {
            dependsOn(project.d8SetupTaskProvider)
        }
        dependsOn(d8Root.setupTaskProvider)
        val v8ExecutablePath = project.provider {
            d8Root.requireConfigured().executablePath.absolutePath
        }
        doFirst {
            systemProperty("javascript.engine.path.V8", v8ExecutablePath.get())
        }
    }

}