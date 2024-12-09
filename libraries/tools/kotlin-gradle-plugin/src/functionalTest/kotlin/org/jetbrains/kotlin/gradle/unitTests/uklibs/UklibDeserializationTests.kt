/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.uklibs

import org.jetbrains.kotlin.gradle.artifacts.uklibsModel.Uklib
import org.jetbrains.kotlin.gradle.util.assertIsInstance
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.test.Test
import kotlin.test.assertEquals

class UklibDeserializationTests {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `forward incompatibility exception - is raised - even if the rest of the json is invalid`() {
        val uklib = temporaryFolder.newFolder("uklib")
        uklib.resolve(Uklib.UMANIFEST_FILE_NAME).writeText(
            """{ "manifestVersion": "banana" }"""
        )
        assertEquals(
            Uklib.Companion.IncompatibleUklibVersion(
                uklib, "banana", Uklib.MAXIMUM_COMPATIBLE_UMANIFEST_VERSION,
            ),
            assertIsInstance<Uklib.Companion.IncompatibleUklibVersion>(
                kotlin.runCatching {
                    Uklib.deserializeFromDirectory(uklib)
                }.exceptionOrNull()
            )
        )
    }

}