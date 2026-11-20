/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import java.nio.file.FileVisitResult
import java.nio.file.Path
import kotlin.io.path.*

/**
 * Path prefixes of the temp directories that JUnit's [org.junit.jupiter.api.io.TempDir] creates,
 * including variations a build script might contain.
 */
internal val testTempDirPathPrefixes: Set<String> by lazy {
    val tempDir = Path(System.getProperty("java.io.tmpdir")).absolute().normalize()
    setOfNotNull(
        tempDir,
        runCatching { tempDir.toRealPath() }.getOrNull(),
    )
        .map { it.resolve("junit") }
        .flatMap {
            setOf(
                it.toString(),
                it.invariantSeparatorsPathString,
                it.toString().replace("\\", "\\\\"),
            )
        }
        .toSet()
}

private val buildScriptOutputDirectoryNames = setOf("build", ".gradle", ".kotlin", "node_modules")

/**
 * Returns `<script>:<line>: <content>` of each build script line that contains a test temp directory.
 */
internal fun findBuildScriptPathReferences(projectPath: Path): List<String> {
    val referencesByScript = sortedMapOf<String, List<String>>()
    projectPath.visitFileTree {
        onPreVisitDirectory { dir, _ ->
            if (dir != projectPath && dir.name in buildScriptOutputDirectoryNames) {
                FileVisitResult.SKIP_SUBTREE
            } else {
                FileVisitResult.CONTINUE
            }
        }
        onVisitFile { file, _ ->
            if (file.name.endsWith(".gradle") || file.name.endsWith(".gradle.kts")) {
                val script = projectPath.relativize(file).invariantSeparatorsPathString
                val references = file.readLines()
                    .withIndex()
                    .filter { (_, line) -> testTempDirPathPrefixes.any { line.contains(it) } }
                    .map { (index, line) ->
                        "$script:${index + 1}: ${line.trim()}"
                    }
                if (references.isNotEmpty()) {
                    referencesByScript[script] = references
                }
            }
            FileVisitResult.CONTINUE
        }
    }
    return referencesByScript.values.flatten()
}

internal fun TestProject.assertBuildScriptsDoNotReferenceTestTempDirectories() {
    val references = findBuildScriptPathReferences(projectPath)
    if (references.isNotEmpty()) {
        throw BuildScriptsReferenceTestTempDirectories(references)
    }
}

private class BuildScriptsReferenceTestTempDirectories(
    val references: List<String>,
) : AssertionError(
    buildString {
        appendLine("Build scripts must not contain test temp directory paths: they make every test's script text unique, so Gradle recompiles it instead of reusing its compiled script cache.")
        appendLine("Instead, use:")
        appendLine("  - a path relative to the project, e.g. file(\"build/…\") or includeBuild(\"../other\")")
        appendLine("  - TestProject.localRepositoryReference for the local Maven repository")
        appendLine("  - TestProject.pathReference(name, path) for any other path")
        appendLine("References:")
        references.forEach { appendLine("  $it") }
    }
)
