/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kpm.idea.proto

import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmContentRoot
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmContentRootImpl
import org.jetbrains.kotlin.tooling.core.emptyExtras
import org.jetbrains.kotlin.tooling.core.extrasKeyOf
import org.jetbrains.kotlin.tooling.core.extrasOf
import org.jetbrains.kotlin.tooling.core.withValue
import org.junit.Test
import java.io.File

class SourceDirectoryTest : AbstractSerializationTest<IdeaKpmContentRoot>() {

    override fun serialize(value: IdeaKpmContentRoot) = value.toByteArray(this)
    override fun deserialize(data: ByteArray) = IdeaKpmContentRoot(data)

    @Test
    fun `serialize - deserialize - sample 0`() = testSerialization(
        IdeaKpmContentRootImpl(
            File("myFile").absoluteFile, type = "myType", extras = emptyExtras()
        )
    )

    @Test
    fun `serialize - deserialize - sample 1`() = testSerialization(
        IdeaKpmContentRootImpl(
            File("myFile").absoluteFile, type = "myType", extras = extrasOf(extrasKeyOf<Int>() withValue 1)
        )
    )
}
