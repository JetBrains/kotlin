/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.proto.tcs

import org.jetbrains.kotlin.gradle.idea.proto.AbstractSerializationTest
import org.jetbrains.kotlin.gradle.idea.proto.generated.tcs.IdeaKotlinSourceCoordinatesProto
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinProjectCoordinates
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinSourceCoordinates
import org.junit.Test

class IdeaKotlinSourceCoordinatesSerializationTest : AbstractSerializationTest<IdeaKotlinSourceCoordinates>() {
    override fun serialize(value: IdeaKotlinSourceCoordinates): ByteArray =
        IdeaKotlinSourceCoordinatesProto(value).toByteArray()

    override fun deserialize(data: ByteArray): IdeaKotlinSourceCoordinates =
        IdeaKotlinSourceCoordinates(IdeaKotlinSourceCoordinatesProto.parseFrom(data))

    @Test
    fun `sample 0`() = testSerialization(
        IdeaKotlinSourceCoordinates(
            project = IdeaKotlinProjectCoordinates(
                buildId = "myBuildId",
                projectPath = "myProjectPath",
                projectName = "myProjectName"
            ),
            sourceSetName = "mySourceSetName"
        )
    )
}
