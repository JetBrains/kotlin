/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.proto.kpm

import org.jetbrains.kotlin.gradle.idea.kpm.IdeaKpmFragmentCoordinates
import org.jetbrains.kotlin.gradle.idea.kpm.IdeaKpmFragmentCoordinatesImpl
import org.jetbrains.kotlin.gradle.idea.kpm.IdeaKpmModuleCoordinatesImpl
import org.jetbrains.kotlin.gradle.idea.proto.AbstractSerializationTest
import org.junit.Test

class FragmentCoordinatesTest : AbstractSerializationTest<IdeaKpmFragmentCoordinates>() {

    override fun serialize(value: IdeaKpmFragmentCoordinates) = value.toByteArray()

    override fun deserialize(data: ByteArray) = IdeaKpmFragmentCoordinates(data)

    @Test
    fun `serialize - deserialize - sample 0`() = testSerialization(
        IdeaKpmFragmentCoordinatesImpl(
            module = IdeaKpmModuleCoordinatesImpl(
                buildId = "buildId",
                projectPath = "projectPath",
                projectName = "projectName",
                moduleName = "moduleName",
                moduleClassifier = null
            ),
            fragmentName = "myFragmentName"
        )
    )
}
