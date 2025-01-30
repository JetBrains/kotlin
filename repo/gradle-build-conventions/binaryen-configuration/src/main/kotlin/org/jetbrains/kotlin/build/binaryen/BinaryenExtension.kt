/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")
package org.jetbrains.kotlin.build.binaryen

import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.binaryen.BinaryenRootEnvSpec

@OptIn(ExperimentalWasmDsl::class)
abstract class BinaryenExtension(
    private val binaryenRoot: BinaryenRootEnvSpec,
) {
    val Project.binaryenVersion: String get() = property("versions.binaryen") as String

    fun Test.setupBinaryen() {
        // use from BinaryenSetupTask after bootstrap
        dependsOn(project.rootProject.tasks.named("kotlinBinaryenSetup"))
        val binaryenExecutablePath = binaryenRoot.executable
        doFirst {
            systemProperty("binaryen.path", binaryenExecutablePath.get())
        }
    }
}