/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.test.tcs

import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinBinaryCapability
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinBinaryCoordinates
import kotlin.test.Test
import kotlin.test.assertEquals

class IdeaKotlinBinaryCoordinatesTest {

    @Test
    fun `test - displayString & identityString - sample 0`() {
        val coordinates = IdeaKotlinBinaryCoordinates("myGroup", "myModule", "1.0.0")
        assertEquals("myGroup:myModule:1.0.0", coordinates.displayString)
        assertEquals("myGroup:myModule:1.0.0", coordinates.identityString)
    }

    @Test
    fun `test - displayString & identityString - sample 1`() {
        val coordinates = IdeaKotlinBinaryCoordinates("myGroup", "myModule", null)
        assertEquals("myGroup:myModule", coordinates.displayString)
        assertEquals("myGroup:myModule", coordinates.identityString)
    }

    @Test
    fun `test - displayString & identityString - sample 2`() {
        val coordinates = IdeaKotlinBinaryCoordinates(
            "myGroup", "myModule", "1.0.0",
            capabilities = setOf(IdeaKotlinBinaryCapability("myGroup", "myModule", "1.0.0"))
        )
        assertEquals("myGroup:myModule:1.0.0", coordinates.displayString)
        assertEquals("myGroup:myModule:1.0.0(myGroup:myModule:1.0.0)", coordinates.identityString)
    }

    @Test
    fun `test - displayString & identityString - sample 3`() {
        val coordinates = IdeaKotlinBinaryCoordinates(
            "myGroup", "myModule", "1.0.0",
            capabilities = setOf(IdeaKotlinBinaryCapability("myGroup", "myModule-foo", "1.0.0"))
        )
        assertEquals("myGroup:myModule-foo:1.0.0", coordinates.displayString)
        assertEquals("myGroup:myModule:1.0.0(myGroup:myModule-foo:1.0.0)", coordinates.identityString)
    }

    @Test
    fun `test - displayString & identityString - sample 4`() {
        val coordinates = IdeaKotlinBinaryCoordinates(
            "myGroup", "myModule", "1.0.0",
            capabilities = setOf(IdeaKotlinBinaryCapability("notMyGroup", "myModule-foo", "1.0.0"))
        )
        assertEquals("myGroup:myModule:1.0.0", coordinates.displayString)

        assertEquals(
            "myGroup:myModule:1.0.0(notMyGroup:myModule-foo:1.0.0)",
            coordinates.identityString
        )
    }

    @Test
    fun `test - displayString & identityString - sample 5`() {
        val coordinates = IdeaKotlinBinaryCoordinates(
            "myGroup", "myModule", "1.0.0",
            capabilities = setOf(
                IdeaKotlinBinaryCapability("notMyGroup", "myModule-foo", "1.0.0"),
                IdeaKotlinBinaryCapability("myGroup", "myModule-foo", "1.0.0")
            )
        )
        assertEquals("myGroup:myModule-foo:1.0.0", coordinates.displayString)

        assertEquals(
            "myGroup:myModule:1.0.0(notMyGroup:myModule-foo:1.0.0, myGroup:myModule-foo:1.0.0)",
            coordinates.identityString
        )
    }

    @Test
    fun `test - displayString & identityString - sample 6`() {
        val coordinates = IdeaKotlinBinaryCoordinates(
            "myGroup", "myModule", "1.0.0",
            capabilities = setOf(
                IdeaKotlinBinaryCapability("myGroup", "myModule-foo", "1.0.0"),
                IdeaKotlinBinaryCapability("notMyGroup", "myModule-foo", "1.0.0"),
                IdeaKotlinBinaryCapability("notMyGroupEither", "myModule-foo", null),
            )
        )
        assertEquals("myGroup:myModule-foo:1.0.0", coordinates.displayString)

        assertEquals(
            "myGroup:myModule:1.0.0(myGroup:myModule-foo:1.0.0, notMyGroup:myModule-foo:1.0.0, notMyGroupEither:myModule-foo)",
            coordinates.identityString
        )
    }

    @Test
    fun `test - displayString & identityString - sample 7`() {
        val coordinates = IdeaKotlinBinaryCoordinates(
            "myGroup", "myModule", "1.0.0", "commonMain",
            capabilities = setOf(
                IdeaKotlinBinaryCapability("myGroup", "myModule-foo", "1.0.0"),
                IdeaKotlinBinaryCapability("notMyGroup", "myModule-foo", "1.0.0"),
                IdeaKotlinBinaryCapability("notMyGroupEither", "myModule-foo", null),
            )
        )
        assertEquals("myGroup:myModule-foo:1.0.0:commonMain", coordinates.displayString)

        assertEquals(
            "myGroup:myModule:1.0.0:commonMain(myGroup:myModule-foo:1.0.0, notMyGroup:myModule-foo:1.0.0, notMyGroupEither:myModule-foo)",
            coordinates.identityString
        )
    }

    @Test
    fun `test - displayString & identityString - sample 8`() {
        val coordinates = IdeaKotlinBinaryCoordinates(
            "myGroup", "myModule", "1.0.0", "commonMain",
            capabilities = setOf(
                IdeaKotlinBinaryCapability("myGroup", "myModule-foo", "1.0.0"),
                IdeaKotlinBinaryCapability("myGroup", "myModule-bar", "1.0.0"),
                IdeaKotlinBinaryCapability("notMyGroup", "myModule-foo", null),
            )
        )
        assertEquals("myGroup:myModule-(foo, bar):1.0.0:commonMain", coordinates.displayString)

        assertEquals(
            "myGroup:myModule:1.0.0:commonMain(myGroup:myModule-foo:1.0.0, myGroup:myModule-bar:1.0.0, notMyGroup:myModule-foo)",
            coordinates.identityString
        )
    }
}
