/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kpm.idea.proto

import org.jetbrains.kotlin.gradle.idea.kpm.IdeaKpmBinaryCoordinatesImpl
import org.jetbrains.kotlin.gradle.idea.kpm.IdeaKpmUnresolvedBinaryDependency
import org.jetbrains.kotlin.gradle.idea.kpm.IdeaKpmUnresolvedBinaryDependencyImpl
import org.jetbrains.kotlin.tooling.core.emptyExtras
import org.jetbrains.kotlin.tooling.core.extrasKeyOf
import org.jetbrains.kotlin.tooling.core.extrasOf
import org.jetbrains.kotlin.tooling.core.withValue
import kotlin.test.Test

class UnresolvedBinaryTest : AbstractSerializationTest<IdeaKpmUnresolvedBinaryDependency>() {

    override fun serialize(value: IdeaKpmUnresolvedBinaryDependency) = value.toByteArray(this)
    override fun deserialize(data: ByteArray) = IdeaKpmUnresolvedBinaryDependency(data)

    @Test
    fun `serialize - deserialize - sample 0`() = testSerialization(
        IdeaKpmUnresolvedBinaryDependencyImpl(
            null, null, emptyExtras()
        )
    )

    @Test
    fun `serialize - deserialize - sample 1`() = testSerialization(
        IdeaKpmUnresolvedBinaryDependencyImpl(
            "1", IdeaKpmBinaryCoordinatesImpl(
                group = "group",
                module = "module",
                version = "version",
                kotlinModuleName = null,
                kotlinFragmentName = null
            ), emptyExtras()
        )
    )

    @Test
    fun `serialize - deserialize - sample 2`() = testSerialization(
        IdeaKpmUnresolvedBinaryDependencyImpl(
            null, null, extrasOf(extrasKeyOf<String>() withValue "myExtraValue")
        )
    )

}
