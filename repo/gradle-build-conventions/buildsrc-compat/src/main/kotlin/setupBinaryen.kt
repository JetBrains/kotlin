/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.targets.js.binaryen.BinaryenRootExtension
import org.jetbrains.kotlin.gradle.targets.js.binaryen.BinaryenRootPlugin

private object BinaryenUtils {
    lateinit var binaryenPlugin: BinaryenRootExtension

    fun useBinaryenPlugin(project: Project) {
        binaryenPlugin = BinaryenRootPlugin.apply(project.rootProject)
    }
}

fun Project.useBinaryenPlugin() {
    BinaryenUtils.useBinaryenPlugin(this)
}

fun Test.setupBinaryen() {
    dependsOn(BinaryenUtils.binaryenPlugin.setupTaskProvider)
    val binaryenExecutablePath = project.provider {
        BinaryenUtils.binaryenPlugin.requireConfigured().executablePath.absolutePath
    }
    doFirst {
        systemProperty("binaryen.path", binaryenExecutablePath.get())
    }
}
