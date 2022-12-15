/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.proto.tcs

import org.jetbrains.kotlin.gradle.idea.proto.AbstractSerializationTest
import org.jetbrains.kotlin.gradle.idea.proto.generated.tcs.IdeaKotlinResolvedBinaryDependencyProto
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinClasspath
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinResolvedBinaryDependency
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.TestIdeaKotlinInstances
import org.jetbrains.kotlin.tooling.core.extrasKeyOf
import org.jetbrains.kotlin.tooling.core.mutableExtrasOf
import org.jetbrains.kotlin.tooling.core.withValue
import java.io.File
import kotlin.test.Test

class IdeaKotlinResolvedBinaryDependencySerializationTest : AbstractSerializationTest<IdeaKotlinResolvedBinaryDependency>() {
    override fun serialize(value: IdeaKotlinResolvedBinaryDependency): ByteArray =
        IdeaKotlinResolvedBinaryDependencyProto(value).toByteArray()

    override fun deserialize(data: ByteArray) =
        IdeaKotlinResolvedBinaryDependency(IdeaKotlinResolvedBinaryDependencyProto.parseFrom(data))

    @Test
    fun `sample 0`() = testSerialization(
        IdeaKotlinResolvedBinaryDependency(
            binaryType = "myBinaryType",
            classpath = IdeaKotlinClasspath(File("myBinaryFile")),
            extras = mutableExtrasOf(extrasKeyOf<String>() withValue "myStringExtras"),
            coordinates = TestIdeaKotlinInstances.simpleBinaryCoordinates
        )
    )

    @Test
    fun `sample 1`() = testSerialization(
        IdeaKotlinResolvedBinaryDependency(
            binaryType = "myBinaryType",
            classpath = IdeaKotlinClasspath(File("myBinaryFile")),
            extras = mutableExtrasOf(),
            coordinates = null
        )
    )
}
