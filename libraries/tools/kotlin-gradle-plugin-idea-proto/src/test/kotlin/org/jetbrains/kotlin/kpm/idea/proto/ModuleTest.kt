/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kpm.idea.proto

import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmModule
import org.jetbrains.kotlin.gradle.kpm.idea.testFixtures.TestIdeaKpmInstances
import kotlin.test.Test

class ModuleTest : AbstractSerializationTest<IdeaKpmModule>() {

    override fun serialize(value: IdeaKpmModule): ByteArray {
        return value.toByteArray(this)
    }

    override fun deserialize(data: ByteArray): IdeaKpmModule {
        return IdeaKpmModule(data)
    }

    @Test
    fun `serialize - deserialize - sample 0`() = testSerialization(TestIdeaKpmInstances.simpleModule)
}
