/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kpm.idea.proto

import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmFragmentCoordinates
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmFragmentCoordinatesImpl
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmModuleCoordinatesImpl
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
