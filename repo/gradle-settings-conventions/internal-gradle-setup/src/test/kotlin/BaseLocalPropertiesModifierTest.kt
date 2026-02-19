/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build

import org.junit.jupiter.api.io.TempDir
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

internal abstract class BaseLocalPropertiesModifierTest {
    @TempDir
    lateinit var workingDir: Path

    protected val localPropertiesFile by lazy {
        workingDir.resolve("local.properties")
    }

    protected val modifier by lazy {
        LocalPropertiesModifier(localPropertiesFile.toFile())
    }

    protected val setupFile = SetupFile(
        mapOf(
            "newProperty1" to "someValue",
            "newProperty2" to "someOtherValue",
            "alreadySetProperty" to "newValue",
        )
    )

    protected fun assertContainsMarkersOnce(content: String) {
        assertContainsExactTimes(content, SYNCED_PROPERTIES_START_LINES, 1)
        assertContainsExactTimes(content, SYNCED_PROPERTIES_END_LINE, 1)
    }

    protected fun Path.propertiesFileContentAssertions(assertions: (String, Properties) -> Unit) {
        val fileContent = Files.readAllLines(this).joinToString("\n")
        try {
            val localProperties = Properties().apply {
                FileInputStream(localPropertiesFile.toFile()).use {
                    load(it)
                }
            }
            assertions(fileContent, localProperties)
        } catch (e: Throwable) {
            println(
                """
                |Some assertions on the properties file have failed.
                |File contents:
                |======
                |$fileContent
                |======
                """.trimMargin()
            )
            throw e
        }
    }
}