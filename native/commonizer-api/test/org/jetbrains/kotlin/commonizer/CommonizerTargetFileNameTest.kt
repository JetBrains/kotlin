/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer

import org.jetbrains.kotlin.commonizer.CommonizerOutputFileLayout.fileName
import org.jetbrains.kotlin.commonizer.CommonizerOutputFileLayout.maxFileNameLength
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

public class CommonizerTargetFileNameTest {

    private val longCommonizerTarget = parseCommonizerTarget(
        buildString {
            append("(")
            append(
                sequence {
                    var i = 0
                    while (true) {
                        yield(i.toString())
                        i++
                    }
                }.take(maxFileNameLength).joinToString(", ")
            )
            append(")")
        }
    )

    @get:Rule
    public val temporaryFolder: TemporaryFolder = TemporaryFolder()

    @Test
    public fun `small targets will use identityString`() {
        val target = parseCommonizerTarget("((a, b), c)")
        assertEquals(target.identityString, target.fileName)
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    public fun `longCommonizerTarget respect maximum fileName length`() {
        assertTrue(
            longCommonizerTarget.identityString.length > maxFileNameLength,
            "Expected test target's identityString to exceed maxFileNameLength"
        )

        assertEquals(
            longCommonizerTarget.fileName.length, maxFileNameLength,
            "Expected test target's fileName to be exactly match the maximum"
        )
    }

    @Test
    public fun `longCommonizerTarget fileName can create new file`() {
        val longCommonizerTargetFile = temporaryFolder.root.resolve(longCommonizerTarget.fileName)
        assertTrue(longCommonizerTargetFile.createNewFile(), "Expected being able to create file $longCommonizerTargetFile")
        longCommonizerTargetFile.writeText(longCommonizerTarget.identityString)
        assertEquals(
            longCommonizerTarget, parseCommonizerTarget(longCommonizerTargetFile.readText()),
            "Expected being able to read and write to $longCommonizerTargetFile"
        )
    }
}
