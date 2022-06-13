/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kpm.idea.proto

import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmBinaryCoordinatesImpl
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmResolvedBinaryDependency
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmResolvedBinaryDependencyImpl
import org.jetbrains.kotlin.gradle.kpm.idea.serialize.IdeaKpmSerializationContext
import org.jetbrains.kotlin.tooling.core.emptyExtras
import org.jetbrains.kotlin.tooling.core.extrasKeyOf
import org.jetbrains.kotlin.tooling.core.extrasOf
import org.jetbrains.kotlin.tooling.core.withValue
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class ResolvedBinaryDependencyTest : AbstractSerializationTest<IdeaKpmResolvedBinaryDependency>() {

    override fun serialize(value: IdeaKpmResolvedBinaryDependency) = value.toByteArray(this)
    override fun deserialize(data: ByteArray) = IdeaKpmResolvedBinaryDependency(data)
    override fun normalize(value: IdeaKpmResolvedBinaryDependency) =
        value.run { this as IdeaKpmResolvedBinaryDependencyImpl }.copy(binaryFile = value.binaryFile.absoluteFile)

    @Test
    fun `serialize - deserialize - sample 0`() = testSerialization(
        IdeaKpmResolvedBinaryDependencyImpl(
            null, binaryType = "binaryType", binaryFile = File("bin"), emptyExtras()
        )
    )

    @Test
    fun `serialize - deserialize - sample 1`() = testSerialization(
        IdeaKpmResolvedBinaryDependencyImpl(
            coordinates = IdeaKpmBinaryCoordinatesImpl(
                group = "group",
                module = "module",
                version = "version",
                kotlinModuleName = null,
                kotlinFragmentName = null
            ),
            binaryType = "binaryType",
            binaryFile = File("bin"),
            extras = extrasOf(extrasKeyOf<Int>() withValue 2411)
        )
    )
}
