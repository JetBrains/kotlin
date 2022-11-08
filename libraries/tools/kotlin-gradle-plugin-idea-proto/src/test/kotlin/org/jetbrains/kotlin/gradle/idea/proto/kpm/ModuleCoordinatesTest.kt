/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.proto.kpm

import org.jetbrains.kotlin.gradle.idea.kpm.IdeaKpmModuleCoordinates
import org.jetbrains.kotlin.gradle.idea.kpm.IdeaKpmModuleCoordinatesImpl
import org.jetbrains.kotlin.gradle.idea.proto.AbstractSerializationTest
import org.jetbrains.kotlin.gradle.idea.testFixtures.kpm.TestIdeaKpmInstances
import kotlin.test.Test

class ModuleCoordinatesTest : AbstractSerializationTest<IdeaKpmModuleCoordinates>() {

    override fun serialize(value: IdeaKpmModuleCoordinates) = value.toByteArray()
    override fun deserialize(data: ByteArray) = IdeaKpmModuleCoordinates(data)

    @Test
    fun `serialize - deserialize - sample 0`() = testSerialization(TestIdeaKpmInstances.simpleModuleCoordinates)

    @Test
    fun `serialize - deserialize - sample 1`() = testSerialization(
        IdeaKpmModuleCoordinatesImpl(
            buildId = "myBuildId",
            projectPath = "myProjectPath",
            projectName = "myProjectName",
            moduleName = "myModuleName",
            moduleClassifier = null
        )
    )
}
