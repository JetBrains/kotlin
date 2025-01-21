/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.binaryen

import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.targets.js.binaryen.BinaryenRootExtension

abstract class BinaryenExtension(
    private val binaryenRoot: BinaryenRootExtension
) {
    val Project.binaryenVersion: String get() = property("versions.binaryen") as String

    fun Test.setupBinaryen() {
        dependsOn(binaryenRoot.setupTaskProvider)
        val binaryenExecutablePath = project.provider {
            binaryenRoot.requireConfigured().executablePath.absolutePath
        }
        doFirst {
            systemProperty("binaryen.path", binaryenExecutablePath.get())
        }
    }
}