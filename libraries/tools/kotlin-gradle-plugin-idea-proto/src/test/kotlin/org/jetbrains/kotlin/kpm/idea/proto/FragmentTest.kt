/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kpm.idea.proto

import org.jetbrains.kotlin.gradle.kpm.idea.*
import org.jetbrains.kotlin.gradle.kpm.idea.testFixtures.TestIdeaKpmInstances
import kotlin.test.Test
import kotlin.test.assertEquals

class FragmentTest : AbstractSerializationTest<IdeaKpmFragment>() {
    override fun serialize(value: IdeaKpmFragment) = value.toByteArray(this)
    override fun deserialize(data: ByteArray) = IdeaKpmFragment(data)

    @Test
    fun `serialize - deserialize - sample 0`() {
        testDeserializedEquals(TestIdeaKpmInstances.simpleFragment)
    }

    @Test
    fun `serialize - deserialize - sample 1`() {
        testDeserializedEquals(TestIdeaKpmInstances.fragmentWithExtras)
    }

    private fun testDeserializedEquals(value: IdeaKpmFragmentImpl) {
        val deserialized = IdeaKpmFragment(value.toByteArray(this))
        val normalized = value.copy(
            dependencies = value.dependencies.map {
                if (it !is IdeaKpmResolvedBinaryDependency) return@map it
                IdeaKpmResolvedBinaryDependencyImpl(
                    coordinates = it.coordinates,
                    binaryType = it.binaryType,
                    binaryFile = it.binaryFile.absoluteFile,
                    extras = it.extras
                )
            },
            languageSettings = (value.languageSettings as IdeaKpmLanguageSettingsImpl).copy(
                compilerPluginClasspath = value.languageSettings.compilerPluginClasspath.map { it.absoluteFile }
            ),
            contentRoots = value.contentRoots
                .map { it as IdeaKpmContentRootImpl }
                .map { it.copy(file = it.file.absoluteFile) }
        )

        assertEquals(normalized, deserialized)
    }
}
