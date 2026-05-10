/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import org.gradle.api.tasks.Sync
import org.jetbrains.kotlin.konan.target.HostManager

/**
 * [Sync] task, that (only on macOS) deletes the destinationDir as the first step.
 *
 * See [KT-85823](https://youtrack.jetbrains.com/issue/KT-85823) for details.
 */
abstract class DeletingSync : Sync() {
    override fun copy() {
        if (HostManager.hostIsMac) {
            deleter.deleteRecursively(destinationDir)
        }
        super.copy()
    }
}