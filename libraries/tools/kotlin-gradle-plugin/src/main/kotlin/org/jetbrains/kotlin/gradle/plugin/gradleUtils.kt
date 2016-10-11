package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.HasConvention
import org.gradle.api.logging.Logger
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.compile.AbstractCompile
import java.io.File

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

internal fun AbstractCompile.mapClasspath(fn: ()->FileCollection) {
    conventionMapping.map("classpath", fn)
}

internal inline fun <reified T : Any> Any.addConvention(name: String, plugin: T) {
    (this as HasConvention).convention.plugins[name] = plugin
}

internal inline fun <reified T : Any> Any.addExtension(name: String, extension: T) =
        (this as ExtensionAware).extensions.add(name, extension)

internal fun Any.getConvention(name: String): Any? =
        (this as HasConvention).convention.plugins[name]

internal fun Logger.kotlinInfo(message: String) {
    this.info("[KOTLIN] $message")
}

internal fun Logger.kotlinDebug(message: String) {
    this.debug("[KOTLIN] $message")
}

internal inline fun Logger.kotlinDebug(message: () -> String) {
    if (isDebugEnabled) {
        kotlinDebug(message())
    }
}