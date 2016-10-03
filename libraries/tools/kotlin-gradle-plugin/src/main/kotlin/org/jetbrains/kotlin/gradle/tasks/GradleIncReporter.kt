package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.logging.Logging
import org.jetbrains.kotlin.incremental.IncReporter
import java.io.File

internal class GradleIncReporter(private val projectRootFile: File) : IncReporter() {
    private val log = Logging.getLogger(GradleIncReporter::class.java)

    override fun report(message: ()->String) {
        log.kotlinDebug(message)
    }

    override fun pathsAsString(files: Iterable<File>): String =
            files.pathsAsStringRelativeTo(projectRootFile)
}

