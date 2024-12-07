/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

public class CommonizerDependencyTest {

    @Test
    public fun `sample identityString`() {
        assertEquals(
            "(a, b, c)::${File("/").canonicalPath}hello.txt",
            TargetedCommonizerDependency(parseCommonizerTarget("(a, b, c)"), File("/hello.txt")).identityString
        )
    }

    @Test
    public fun `test serialize deserialize`() {
        assertEquals(
            parseCommonizerDependency(NonTargetedCommonizerDependency(File("hello.txt")).identityString),
            NonTargetedCommonizerDependency(File("hello.txt").canonicalFile)
        )

        assertEquals(
            parseCommonizerDependency(TargetedCommonizerDependency(parseCommonizerTarget("((a,b), c)"), File("hello.txt")).identityString),
            TargetedCommonizerDependency(parseCommonizerTarget("((a,b), c)"), File("hello.txt").canonicalFile)
        )
    }
}
