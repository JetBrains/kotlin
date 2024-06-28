/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.proto.tcs

import org.jetbrains.kotlin.gradle.idea.proto.AbstractSerializationTest
import org.jetbrains.kotlin.gradle.idea.proto.generated.tcs.IdeaKotlinProjectCoordinatesProto
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinProjectCoordinates
import kotlin.test.Test

class IdeaKotlinProjectCoordinatesSerializationTest : AbstractSerializationTest<IdeaKotlinProjectCoordinates>() {
    override fun serialize(value: IdeaKotlinProjectCoordinates): ByteArray {
        return IdeaKotlinProjectCoordinatesProto(value).toByteArray()
    }

    override fun deserialize(data: ByteArray): IdeaKotlinProjectCoordinates {
        return IdeaKotlinProjectCoordinates(IdeaKotlinProjectCoordinatesProto.parseFrom(data))
    }

    @Test
    fun `test - only buildId provided`() = testSerialization(
        @Suppress("DEPRECATION")
        IdeaKotlinProjectCoordinates(
            buildId = "myBuildId",
            projectPath = "myProjectPath",
            projectName = "myProjetName"
        )
    )

    @Test
    fun `test - only buildPath and buildName provided`() = testSerialization(
        IdeaKotlinProjectCoordinates(
            buildPath = "myBuildId",
            buildName = "myBuildName",
            projectPath = "myProjectPath",
            projectName = "myProjetName"
        )
    )
}