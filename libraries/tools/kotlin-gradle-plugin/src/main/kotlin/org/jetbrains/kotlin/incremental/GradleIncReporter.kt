package org.jetbrains.kotlin.incremental

import org.gradle.api.logging.Logging
import org.jetbrains.kotlin.gradle.plugin.kotlinDebug
import java.io.File

internal class GradleIncReporter(private val projectRootFile: File) : IncReporter() {
    private val log = Logging.getLogger(GradleIncReporter::class.java)

    override fun report(message: ()->String) {
        log.kotlinDebug(message)
    }

    override fun pathsAsString(files: Iterable<File>): String =
            files.pathsAsStringRelativeTo(projectRootFile)
}

