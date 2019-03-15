/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.incremental

import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.jetbrains.kotlin.incremental.ChangedFiles
import java.io.File
import java.util.*

internal fun ChangedFiles(taskInputs: IncrementalTaskInputs): ChangedFiles {
    if (!taskInputs.isIncremental) return ChangedFiles.Unknown()

    val modified = ArrayList<File>()
    val removed = ArrayList<File>()

    taskInputs.outOfDate { modified.add(it.file) }
    taskInputs.removed { removed.add(it.file) }

    return ChangedFiles.Known(modified, removed)
}
