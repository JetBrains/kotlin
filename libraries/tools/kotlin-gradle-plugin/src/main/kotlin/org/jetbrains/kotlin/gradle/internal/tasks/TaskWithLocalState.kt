/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.tasks

import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.utils.outputsCompatible

internal interface TaskWithLocalState : Task {
    fun localStateDirectories(): FileCollection
}

internal fun TaskWithLocalState.allOutputFiles(): FileCollection =
    outputsCompatible.files + localStateDirectories()
