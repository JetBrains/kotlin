/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.proto.tcs

import org.jetbrains.kotlin.gradle.idea.proto.AbstractSerializationTest
import org.jetbrains.kotlin.gradle.idea.proto.generated.tcs.IdeaKotlinSourceDependencyProto
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinSourceDependency
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.TestIdeaKotlinInstances
import org.jetbrains.kotlin.tooling.core.mutableExtrasOf
import org.jetbrains.kotlin.tooling.core.toMutableExtras
import kotlin.test.Test

class IdeaKotlinSourceDependencySerializationTest : AbstractSerializationTest<IdeaKotlinSourceDependency>() {
    override fun serialize(value: IdeaKotlinSourceDependency): ByteArray = IdeaKotlinSourceDependencyProto(value).toByteArray()

    override fun deserialize(data: ByteArray): IdeaKotlinSourceDependency =
        IdeaKotlinSourceDependency(IdeaKotlinSourceDependencyProto.parseFrom(data))

    @Test
    fun `sample 0`() = testSerialization(
        IdeaKotlinSourceDependency(
            coordinates = TestIdeaKotlinInstances.simpleSourceCoordinates,
            type = IdeaKotlinSourceDependency.Type.Regular,
            extras = TestIdeaKotlinInstances.extrasWithIntAndStrings.toMutableExtras()
        )
    )

    @Test
    fun `sample 1`() = testSerialization(
        IdeaKotlinSourceDependency(
            coordinates = TestIdeaKotlinInstances.simpleSourceCoordinates,
            type = IdeaKotlinSourceDependency.Type.Friend,
            extras = mutableExtrasOf()
        )
    )

    @Test
    fun `sample 2`() = testSerialization(
        IdeaKotlinSourceDependency(
            coordinates = TestIdeaKotlinInstances.simpleSourceCoordinates,
            type = IdeaKotlinSourceDependency.Type.DependsOn,
            extras = mutableExtrasOf()
        )
    )
}
