/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.process.ExecOperations
import org.gradle.process.ExecResult
import org.gradle.process.ExecSpec
import org.jetbrains.kotlin.konan.target.PlatformManager
import org.jetbrains.kotlin.konan.util.DependencyProcessor

fun execLlvmUtility(project: Project, utility: String, action: Action<in ExecSpec>): ExecResult {
    val llvmBinDirectory = "${project.platformManager.hostPlatform.absoluteLlvmHome}/bin"
    return project.exec(Action<ExecSpec> {
        action.execute(this)
        executable = "$llvmBinDirectory/$utility"
    })
}

fun execLlvmUtility(project: Project, utility: String, closure: Closure<in ExecSpec>): ExecResult {
    return execLlvmUtility(project, utility) { project.configure(this, closure) }
}

fun ExecOperations.execLlvmUtility(platformManager: PlatformManager, utility: String, action: Action<in ExecSpec>): ExecResult {
    val llvmBinDirectory = "${platformManager.hostPlatform.absoluteLlvmHome}/bin"
    return exec {
        action.execute(this)
        executable = "$llvmBinDirectory/$utility"
    }
}