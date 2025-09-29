/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalWasmDsl::class)

package org.jetbrains.kotlin.build.d8

import SystemPropertyClasspathProvider
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.newInstance
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.wasm.d8.D8EnvSpec

abstract class D8Extension(
    private val project: Project,
    private val d8envSpec: D8EnvSpec,
) {
    val v8Version: String
        get() = project.property("versions.v8") as String

    val v8ExecutablePath: Provider<String> = d8envSpec.executable.also {
        project.extra["javascript.engine.path.V8"] = it
    }

    fun Test.setupV8() {
        with(d8envSpec) {
            dependsOn(project.d8SetupTaskProvider)
        }

        jvmArgumentProviders += this.project.objects.newInstance<SystemPropertyClasspathProvider>().apply {
            classpath.from(v8ExecutablePath)
            property.set("javascript.engine.path.V8")
        }
    }

}