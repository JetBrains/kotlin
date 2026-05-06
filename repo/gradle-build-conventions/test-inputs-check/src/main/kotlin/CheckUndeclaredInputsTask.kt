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
        val declared = declaredInputs.get().toHashSet()
        val accessedPaths = LinkedHashSet<Path>()
        val undeclaredFiles = LinkedHashMap<Path, AccessedFile>()

        RecordingFile(jfrFile.toPath()).use { recording ->
            while (recording.hasMoreEvents()) {
                val event = recording.readEvent()
                if (event.eventType.name != "jdk.FileRead") continue
                val rawPath = event.getString("path") ?: continue

                var path = Paths.get(rawPath)
                if (!path.isAbsolute) path = projectPath.resolve(path)
                path = Paths.get(path.toFile().canonicalPath)

                if (!path.startsWith(rootPath)) continue
                if (path.startsWith(buildPath)) continue
                if (!accessedPaths.add(path)) continue

                if (path !in declared) {
                    undeclaredFiles[path] = AccessedFile(path = path, stacktrace = event.stackTrace.frames)
                }
            }
        }

        println("Accessed files (${accessedPaths.size})")
        println("Declared inputs (${declared.size})")

        if (undeclaredFiles.isNotEmpty()) {
            reportFile.parentFile.mkdirs()
            reportFile.writeText(buildHtmlReport(undeclaredFiles.values.toList(), projectPath))

            error(buildString {
                appendLine("Undeclared inputs found! (${undeclaredFiles.size})")
                appendLine("See HTML report for stacktraces: ${reportFile.toURI()}")
                appendLine("First 100 files:")
                undeclaredFiles.keys.take(100).forEach { appendLine(it) }
            })
        }
    }
}
