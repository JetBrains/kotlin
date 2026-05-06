import jdk.jfr.consumer.RecordingFile
import org.gradle.api.DefaultTask
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.nio.file.Path
import java.nio.file.Paths

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

abstract class CheckUndeclaredInputsTask : DefaultTask() {

    @get:Input
    abstract val declaredInputs: ListProperty<Path>

    @Internal
    val jfrFile = project.layout.buildDirectory.dir("jfr").get().asFile.resolve("test.jfr")

    @Internal
    val projectPath = project.projectDir.toPath()

    @Internal
    val rootPath = project.rootDir.toPath()

    @Internal
    val buildPath = project.layout.buildDirectory.get().asFile.toPath()

    @Internal
    val reportFile = project.layout.buildDirectory.file("undeclared-inputs.html").get().asFile

    @TaskAction
    fun execute() {
        val accessedFiles = RecordingFile.readAllEvents(jfrFile.toPath())
            .filter { it.eventType.name == "jdk.FileRead" }
            .filter { it.getString("path") != null }
            .map {
                AccessedFile(
                    path = Paths.get(it.getString("path")!!),
                    stacktrace = it.stackTrace.frames
                )
            }
            .map { it.mapPath { path -> if (!path.isAbsolute) projectPath.resolve(path) else path } }
            .map { it.mapPath { path -> Paths.get(path.toFile().canonicalPath) } }
            .filter { it.path.startsWith(rootPath) }
            .filterNot { it.path.startsWith(buildPath) }
            .associateBy { it.path }

        val accessedPaths = accessedFiles.keys
        val undeclaredInputs = accessedPaths - declaredInputs.get()
        val undeclaredInputFiles = undeclaredInputs.map { accessedFiles[it] }

        println("Accessed files (${accessedPaths.size}):")
        accessedPaths.forEach { println(it) }
        println("Declared inputs (${declaredInputs.get().size})")

        if (undeclaredInputFiles.isNotEmpty()) {
            reportFile.parentFile.mkdirs()
            reportFile.writeText(buildHtmlReport(undeclaredInputFiles.filterNotNull(), projectPath))

            error(buildString {
                appendLine("Undeclared inputs found! (${undeclaredInputFiles.size})")
                appendLine("See HTML report for stacktraces: ${reportFile.toURI()}")
                appendLine("First 100 files:")
                undeclaredInputs.take(100).forEach { appendLine(it) }
            })
        }
    }
}
