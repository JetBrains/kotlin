/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kpm.idea.proto

import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmCompilationOutput
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmCompilationOutputImpl
import org.jetbrains.kotlin.gradle.kpm.idea.testFixtures.TestIdeaKpmInstances
import kotlin.test.Test

class CompilationOutputTest : AbstractSerializationTest<IdeaKpmCompilationOutput>() {

    override fun serialize(value: IdeaKpmCompilationOutput): ByteArray = value.toByteArray()
    override fun deserialize(data: ByteArray): IdeaKpmCompilationOutput = IdeaKpmCompilationOutput(data)

    @Test
    fun `serialize - deserialize - sample 0`() {
        testSerialization(TestIdeaKpmInstances.simpleCompilationOutput)
    }

    @Test
    fun `serialize - deserialize - sample 1`() = testSerialization(
        IdeaKpmCompilationOutputImpl(
            classesDirs = emptySet(),
            resourcesDir = null
        )
    )
}
