/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.internal.build.instrumentation

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import java.io.File
import kotlin.system.measureTimeMillis

open class NotNullVerifierPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.withType(JavaCompile::class.java).configureEach { javaCompile ->
            addNotNullInstrumentationToOutputClasses(javaCompile)
        }
    }
}

internal data class FileSnapshot(val file: File, val lastModified: Long, val size: Long) {
    constructor(file: File) : this(file = file, lastModified = file.lastModified(), size = file.length())
}

internal fun addNotNullInstrumentationToOutputClasses(javaCompile: JavaCompile) {
    val snapshots = HashMap<File, FileSnapshot>()
    var walkTimeMs = 0L
    fun forEachOutputClassFile(fn: (File) -> Unit) {
        walkTimeMs += measureTimeMillis {
            for (file in javaCompile.destinationDir.walk().filter { it.isFile && it.endsWith(".class") }) {
                fn(file)
            }
        }
    }

    javaCompile.doFirst {
        forEachOutputClassFile {
            snapshots[it] = FileSnapshot(it)
        }
    }

    javaCompile.doLast("instrument java classes") {
        forEachOutputClassFile {
            val curr = FileSnapshot(it)
            val prev = snapshots[it]

            if (curr == prev) {
                // we want to instrument only files with different snapshots
                snapshots.remove(it)
            }
        }
        val filesToInstrument = snapshots.keys
        val log = javaCompile.logger
        log.info("Determined files to instrument in $walkTimeMs ms")
        log.info("Started adding not-null instrumentation to ${filesToInstrument.size} class files...")
        if (log.isDebugEnabled) {
            log.debug("Files to be instrumented: \n${filesToInstrument.map { it.path }.sorted().joinToString("\n")}")
        }
        val instrumentationTimeMs = measureTimeMillis {
            addNotNullVerification(
                project = javaCompile.project,
                filesToProcess = filesToInstrument,
                classpath = javaCompile.classpath.files,
                javaHome = javaCompile.options.forkOptions.javaHome
                    ?: File(System.getProperty("java.home")!!)
            )
        }
        log.info("Finished adding not-null instrumentation to ${filesToInstrument.size} class files in $instrumentationTimeMs ms")
    }
}
