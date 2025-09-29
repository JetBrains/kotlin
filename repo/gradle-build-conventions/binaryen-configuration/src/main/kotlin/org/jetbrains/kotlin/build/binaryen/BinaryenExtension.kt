/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.binaryen

import SystemPropertyClasspathProvider
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.newInstance
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenEnvSpec

@OptIn(ExperimentalWasmDsl::class)
abstract class BinaryenExtension(
    private val project: Project,
    private val binaryen: BinaryenEnvSpec,
) {
    val binaryenVersion: String
        get() = project.property("versions.binaryen") as String

    val binaryenExecutablePath: Provider<String> = binaryen.executable.also {
        project.extra["binaryen.path"] = it
    }

    fun Test.setupBinaryen() {
        with(binaryen) {
            dependsOn(project.binaryenSetupTaskProvider)
        }

        jvmArgumentProviders += this.project.objects.newInstance<SystemPropertyClasspathProvider>().apply {
            classpath.from(binaryenExecutablePath)
            property.set("binaryen.path")
        }
    }
}