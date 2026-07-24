/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createFile
import kotlin.test.assertEquals

class NpmDependencyTest {

    @Test
    fun `directoryNpmDependency - expect version uses absolute real path`(
        @TempDir
        tempDir: Path,
    ) {
        val project = ProjectBuilder.builder().build()

        val npmDep =
            directoryNpmDependency(
                project.objects,
                NpmDependency.Scope.NORMAL,
                "test",
                tempDir.toFile(),
            )

        assertEquals(
            "file:" + tempDir.toRealPath().absolutePathString(),
            npmDep.version,
        )
    }

    @Test
    fun `directoryNpmDependency - expect failure if not a directory`(
        @TempDir
        tempDir: Path,
    ) {
        val file = tempDir.resolve("file.txt")
        file.createFile()

        val project = ProjectBuilder.builder().build()

        assertThrows<IllegalStateException>("Dependency on local path should point on directory but $file found") {
            directoryNpmDependency(
                project.objects,
                NpmDependency.Scope.NORMAL,
                "test",
                file.toFile(),
            )
        }
    }
}
