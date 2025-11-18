/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.xcode.tasks

import org.gradle.api.tasks.TaskAction

/**
 * Lists all available Simulator Runtimes and Device Types.
 * Useful for debugging configuration strings.
 */
abstract class ListXcodeInfoTask : AbstractXcodeTask() {
    @TaskAction
    fun list() {
        logger.lifecycle("Fetching Xcode Runtime Information...")
        val output = runCommand("xcrun", "simctl", "list", "runtimes")
        logger.lifecycle(output)
    }
}