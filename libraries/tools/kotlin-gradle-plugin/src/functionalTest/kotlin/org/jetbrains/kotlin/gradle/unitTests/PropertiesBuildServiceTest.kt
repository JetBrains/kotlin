/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.plugin.PropertiesBuildService
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.registerMinimalVariantImplementationFactoriesForTests
import org.junit.Assert.assertEquals
import org.junit.Test

class PropertiesBuildServiceTest {

    @Test
    fun `testPrecedenceOrder`() {
        val project = buildProject()
        project.extraProperties.apply {
            set("a", "extra")
            set("b", "extra")
            set("x", 1) // Test the case where extra property is not a String
        }
        // TODO: Find a way to test Gradle properties (it seems that more setup is needed for Gradle to pick up the contents of
        //  `gradle.properties` in this test).
        project.projectDir.resolve("local.properties").writeText(
            """
            a = local
            c = local
            x = "local"
            """.trimIndent()
        )

        project.gradle.registerMinimalVariantImplementationFactoriesForTests()
        val properties = PropertiesBuildService.registerIfAbsent(project).get()

        assertEquals("extra", properties.get("a", project))
        assertEquals("extra", properties.get("b", project))
        assertEquals("local", properties.get("c", project))
        assertEquals(null, properties.get("d", project))
        assertEquals("\"local\"", properties.get("x", project))
    }

}
