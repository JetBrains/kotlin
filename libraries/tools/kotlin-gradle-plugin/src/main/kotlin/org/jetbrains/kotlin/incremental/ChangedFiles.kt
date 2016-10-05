package org.jetbrains.kotlin.incremental

import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import java.io.File
import java.util.*

internal sealed class ChangedFiles {
    class Known(val modified: List<File>, val removed: List<File>) : ChangedFiles()
    class Unknown : ChangedFiles()
}

internal fun ChangedFiles(taskInputs: IncrementalTaskInputs): ChangedFiles {
    if (!taskInputs.isIncremental) return ChangedFiles.Unknown()

    val modified = ArrayList<File>()
    val removed = ArrayList<File>()

    taskInputs.outOfDate { modified.add(it.file) }
    taskInputs.removed { removed.add(it.file) }

    return ChangedFiles.Known(modified, removed)
}
