/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.uklibs

import org.gradle.kotlin.dsl.support.unzipTo
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.serialization.IncompatibleUklibVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.Uklib
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.UklibFragment
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.UklibModule
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.serialization.deserializeUklibFromDirectory
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.serialization.serializeToZipArchive
import org.jetbrains.kotlin.gradle.testing.PrettyPrint
import org.jetbrains.kotlin.gradle.testing.prettyPrinted
import org.jetbrains.kotlin.gradle.util.assertIsInstance
import org.jetbrains.kotlin.incremental.createDirectory
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
            IncompatibleUklibVersion(
                uklib, "banana", Uklib.MAXIMUM_COMPATIBLE_UMANIFEST_VERSION,
            ),
            assertIsInstance<IncompatibleUklibVersion>(
                kotlin.runCatching {
                    deserializeUklibFromDirectory(uklib)
                }.exceptionOrNull()
            )
        )
    }

    @Test
    fun `uklib back and forth serialization`() {
        val temporaryDirectory = temporaryFolder.newFolder("temporary")
        val outputUklibArchive = temporaryFolder.newFolder("uklib").resolve("output.uklib")

        Uklib(
            UklibModule(
                setOf(
                    UklibFragment(
                        identifier = "iosArm64Main",
                        attributes = setOf("ios_arm64"),
                        file = temporaryDirectory.resolve("iosArm64Main").also {
                            it.createDirectory()
                            it.resolve("file").writeText("iosArm64MainContent")
                        }
                    ),
                    UklibFragment(
                        identifier = "commonMain",
                        attributes = setOf("ios_arm64", "jvm"),
                        file = temporaryDirectory.resolve("commonMain").also {
                            it.createDirectory()
                            it.resolve("file").writeText("commonMainContent")
                        }
                    ),
                    UklibFragment(
                        identifier = "jvmMain",
                        attributes = setOf("jvm"),
                        file = temporaryDirectory.resolve("jvmMain").also {
                            it.createDirectory()
                            it.resolve("file").writeText("jvmMainContent")
                        }
                    ),
                )
            ),
            manifestVersion = Uklib.MAXIMUM_COMPATIBLE_UMANIFEST_VERSION,
        ).serializeToZipArchive(
            outputZip = outputUklibArchive,
            temporariesDirectory = temporaryFolder.newFolder("temporaries")
        )

        val uklibDirectory = temporaryFolder.newFolder("unzippedUklib")
        unzipTo(
            uklibDirectory,
            outputUklibArchive
        )

        val uklib = deserializeUklibFromDirectory(uklibDirectory)

        data class TestFragment(
            val identifier: String,
            val attributes: Set<String>,
            val contents: String,
        )

        assertEquals<PrettyPrint<List<TestFragment>>>(
            uklib.module.fragments.map {
                TestFragment(
                    it.identifier,
                    it.attributes,
                    it.files.single().resolve("file").readText(),
                )
            }.prettyPrinted, mutableListOf<TestFragment>(
                TestFragment(
                    attributes = mutableSetOf(
                        "ios_arm64",
                        "jvm",
                    ),
                    contents = "commonMainContent",
                    identifier = "commonMain",
                ),
                TestFragment(
                    attributes = mutableSetOf(
                        "ios_arm64",
                    ),
                    contents = "iosArm64MainContent",
                    identifier = "iosArm64Main",
                ),
                TestFragment(
                    attributes = mutableSetOf(
                        "jvm",
                    ),
                    contents = "jvmMainContent",
                    identifier = "jvmMain",
                ),
            )
                .prettyPrinted
        )
    }

}