/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.proto.tcs

import org.jetbrains.kotlin.gradle.idea.proto.AbstractSerializationTest
import org.jetbrains.kotlin.gradle.idea.proto.generated.tcs.IdeaKotlinUnresolvedBinaryDependencyProto
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinUnresolvedBinaryDependency
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.TestIdeaKotlinInstances
import org.jetbrains.kotlin.tooling.core.toMutableExtras
import kotlin.test.Test

class IdeaKotlinUnresolvedBinaryDependencySerializationTest : AbstractSerializationTest<IdeaKotlinUnresolvedBinaryDependency>() {
    override fun serialize(value: IdeaKotlinUnresolvedBinaryDependency): ByteArray =
        IdeaKotlinUnresolvedBinaryDependencyProto(value).toByteArray()

    override fun deserialize(data: ByteArray) =
        IdeaKotlinUnresolvedBinaryDependency(IdeaKotlinUnresolvedBinaryDependencyProto.parseFrom(data))

    @Test
    fun `sample 0`() = testSerialization(
        IdeaKotlinUnresolvedBinaryDependency(
            cause = "myCause",
            coordinates = TestIdeaKotlinInstances.simpleBinaryCoordinates,
            extras = TestIdeaKotlinInstances.extrasWithIntAndStrings.toMutableExtras()
        )
    )

    @Test
    fun `sample 1`() = testSerialization(
        IdeaKotlinUnresolvedBinaryDependency(
            cause = null,
            coordinates = TestIdeaKotlinInstances.simpleBinaryCoordinates,
            extras = TestIdeaKotlinInstances.extrasWithIntAndStrings.toMutableExtras()
        )
    )

    @Test
    fun `sample 2`() = testSerialization(
        IdeaKotlinUnresolvedBinaryDependency(
            cause = "myCause",
            coordinates = null,
            extras = TestIdeaKotlinInstances.extrasWithIntAndStrings.toMutableExtras()
        )
    )
}
