/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.test.documentable

import org.jetbrains.dokka.analysis.test.api.kotlinJvmTestProject
import org.jetbrains.dokka.analysis.test.api.parse
import org.jetbrains.dokka.model.doc.*
import kotlin.test.Test
import kotlin.test.assertEquals

class SpecialCharacterTest {

    @Test
    fun `should be able to parse email`() {
        val project = kotlinJvmTestProject {
            ktFile("Klass.kt") {
                +"""
                    /**
                     * <me@mail.com>
                     */
                    class Klass
                """
            }
        }

        val module = project.parse()

        val pkg = module.packages.single()
        val cls = pkg.classlikes.single()

        assertEquals("Klass", cls.name)

        val text = P(
            listOf(
                Text("<", params = mapOf("content-type" to "html")),
                Text("me@mail.com"),
                Text(">", params = mapOf("content-type" to "html"))
            )
        )
        val description = Description(CustomDocTag(listOf(text), name = "MARKDOWN_FILE"))

        assertEquals(
            DocumentationNode(listOf(description)),
            cls.documentation.values.single(),
        )
    }
}
