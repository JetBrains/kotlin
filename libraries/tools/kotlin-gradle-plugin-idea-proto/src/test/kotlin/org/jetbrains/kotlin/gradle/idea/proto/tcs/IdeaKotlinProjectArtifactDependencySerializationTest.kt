/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.proto.tcs

import org.jetbrains.kotlin.gradle.idea.proto.AbstractSerializationTest
import org.jetbrains.kotlin.gradle.idea.proto.generated.tcs.IdeaKotlinProjectArtifactDependencyProto
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinProjectArtifactDependency
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.TestIdeaKotlinInstances
import kotlin.test.Test

class IdeaKotlinProjectArtifactDependencySerializationTest : AbstractSerializationTest<IdeaKotlinProjectArtifactDependency>() {
    override fun serialize(value: IdeaKotlinProjectArtifactDependency): ByteArray {
        return IdeaKotlinProjectArtifactDependencyProto(value).toByteArray()
    }

    override fun deserialize(data: ByteArray): IdeaKotlinProjectArtifactDependency {
        return IdeaKotlinProjectArtifactDependency(IdeaKotlinProjectArtifactDependencyProto.parseFrom(data))
    }

    @Test
    fun `sample 0`() = testSerialization(TestIdeaKotlinInstances.simpleProjectArtifactDependency)
}
