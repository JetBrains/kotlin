/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer

import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals

class CommonizerDependencyTest {

    @Test
    fun `sample identityString`(@TempDir tempDir: Path) {
        val dependencyFile = tempDir.resolve("hello.txt")
        assertEquals(
            "(a, b, c)::${dependencyFile.toFile().canonicalPath}",
            TargetedCommonizerDependency(parseCommonizerTarget("(a, b, c)"), dependencyFile.toFile().absoluteFile).identityString
        )
    }

    @Test
    fun `test serialize deserialize`(@TempDir tempDir: Path) {
        val dependencyFile = tempDir.resolve("hello.txt")

        assertEquals(
            parseCommonizerDependency(NonTargetedCommonizerDependency(dependencyFile.toFile()).identityString),
            NonTargetedCommonizerDependency(dependencyFile.toFile().canonicalFile)
        )

        assertEquals(
            parseCommonizerDependency(
                TargetedCommonizerDependency(parseCommonizerTarget("((a,b), c)"), dependencyFile.toFile()).identityString
            ), TargetedCommonizerDependency(parseCommonizerTarget("((a,b), c)"), dependencyFile.toFile().canonicalFile)
        )
    }
}
