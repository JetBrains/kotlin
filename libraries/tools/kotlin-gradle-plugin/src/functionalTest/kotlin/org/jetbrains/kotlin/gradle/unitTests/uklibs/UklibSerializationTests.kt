/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.uklibs

import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.Uklib
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.UklibFragment
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.UklibModule
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.serialization.MissingUklibFragmentFile
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.serialization.IncompatibleUklibFragmentFile
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.serialization.serializeToZipArchive
import org.jetbrains.kotlin.gradle.util.assertIsInstance
import org.jetbrains.kotlin.incremental.createDirectory
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.test.Test
import kotlin.test.assertEquals

class UklibSerializationTests {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `missing file`() {
        val temp = temporaryFolder.newFolder("temp")
        val missing = temp.resolve("doesnt_exist")

        assertEquals(
            MissingUklibFragmentFile(missing),
            assertIsInstance(
                runCatching {
                    Uklib(
                        UklibModule(
                            hashSetOf(
                                UklibFragment(
                                    "a",
                                    attributes = hashSetOf("a"),
                                    file = { missing },
                                )
                            )
                        ),
                        Uklib.CURRENT_UMANIFEST_VERSION,
                    ).serializeToZipArchive(
                        temp.resolve("output.zip"),
                        temp,
                    )
                }.exceptionOrNull()
            )
        )
    }

    @Test
    fun `invalid file`() {
        val temp = temporaryFolder.newFolder("temp")
        val invalid = temp.resolve("foo.invalid")
        invalid.createNewFile()

        assertEquals(
            IncompatibleUklibFragmentFile(invalid),
            assertIsInstance(
                runCatching {
                    Uklib(
                        UklibModule(
                            hashSetOf(
                                UklibFragment(
                                    "a",
                                    attributes = hashSetOf("a"),
                                    file = { invalid },
                                )
                            )
                        ),
                        Uklib.CURRENT_UMANIFEST_VERSION,
                    ).serializeToZipArchive(
                        temp.resolve("output.zip"),
                        temp,
                    )
                }.exceptionOrNull()
            )
        )
    }

    // Just check that we can don't fail to pack a directory
    @Test
    fun `pack directory`() {
        val temp = temporaryFolder.newFolder("temp")
        val dir = temp.resolve("foo")
        dir.createDirectory()
        dir.resolve("marker").writeText("test")

        Uklib(
            UklibModule(
                hashSetOf(
                    UklibFragment(
                        "a",
                        attributes = hashSetOf("a"),
                        file = { dir },
                    )
                )
            ),
            Uklib.CURRENT_UMANIFEST_VERSION,
        ).serializeToZipArchive(temp.resolve("output.zip"), temp)
    }

}