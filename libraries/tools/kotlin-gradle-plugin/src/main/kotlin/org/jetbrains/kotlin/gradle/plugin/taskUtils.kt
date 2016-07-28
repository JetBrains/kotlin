package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.AbstractTask
import org.gradle.api.tasks.compile.AbstractCompile
import java.io.File
import kotlin.reflect.KProperty

internal fun AbstractCompile.appendClasspathDynamically(file: File) {
    var added = false

    doFirst {
        if (file !in classpath) {
            classpath += project.files(file)
            added = true
        }
    }
    doLast {
        if (added) {
            classpath -= project.files(file)
        }
    }
}

// Extends finalizedBy clause so that finalizing task does not run if finalized task failed
internal fun Task.finalizedByIfNotFailed(finalizer: Task) {
    finalizer.onlyIf { this@finalizedByIfNotFailed.state.failure == null }
    this.finalizedBy(finalizer)
}

fun AbstractCompile.mapClasspath(fn: ()->FileCollection) {
    conventionMapping.map("classpath", fn)
}