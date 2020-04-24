/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle

import org.jetbrains.kotlin.idea.scripting.gradle.importing.KotlinDslScriptModel
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import kotlin.test.assertEquals

class KotlinDslScriptModelsTest {
    @Test
    fun write() {
        val data = GradleImportedBuildRootData(
            listOf("a", "b", "c"),
            listOf(
                KotlinDslScriptModel(
                    "a",
                    GradleKotlinScriptConfigurationInputs("b", 1),
                    listOf("c", "a", "b"),
                    listOf("b", "c", "a"),
                    listOf("i", "c", "b"),
                    listOf()
                ),
                KotlinDslScriptModel(
                    "a",
                    GradleKotlinScriptConfigurationInputs("b", 1),
                    listOf("c", "a", "b"),
                    listOf("b", "c", "a"),
                    listOf("i", "c", "b"),
                    listOf()
                )
            )
        )

        val buffer = ByteArrayOutputStream()
        writeKotlinDslScriptModels(DataOutputStream(buffer), data)

        val restored = readKotlinDslScriptModels(DataInputStream(ByteArrayInputStream(buffer.toByteArray())))

        assertEquals(data.toString(), restored.toString())
    }
}
