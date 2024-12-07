/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.internal.properties.PropertiesBuildService
import org.jetbrains.kotlin.gradle.internal.properties.propertiesService
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.registerMinimalVariantImplementationFactoriesForTests
import org.junit.Assert.assertEquals
import org.junit.Test

class PropertiesBuildServiceTest {

    @Test
    fun testPrecedenceOrder() {
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
        assertEquals("1", properties.get("x", project))
    }

    @Test
    fun testDifferentSubProjectsExtraProperties() {
        val rootProject = buildProject()
        val subProject1 = buildProject(projectBuilder = {
            withParent(rootProject)
            withName("sub-project-1")
        })
        val subProject2 = buildProject(projectBuilder = {
            withParent(rootProject)
            withName("sub-project-2")
        })
        rootProject.gradle.registerMinimalVariantImplementationFactoriesForTests()
        subProject1.gradle.registerMinimalVariantImplementationFactoriesForTests()
        subProject2.gradle.registerMinimalVariantImplementationFactoriesForTests()

        rootProject.extraProperties.set("a", "root")
        subProject1.extraProperties.set("a", "subProject1")
        subProject2.extraProperties.set("a", "subProject2")

        val properties = PropertiesBuildService.registerIfAbsent(rootProject).get()

        assertEquals("root", properties.property("a", rootProject).get())
        assertEquals("subProject1", properties.property("a", subProject1).get())
        assertEquals("subProject2", properties.property("a", subProject2).get())
    }

    @Test
    fun testExtraPropertyMemoizationOnFirstRead() {
        val rootProject = buildProject()
        rootProject.gradle.registerMinimalVariantImplementationFactoriesForTests()

        rootProject.extraProperties.set("a", "root")

        val properties = PropertiesBuildService.registerIfAbsent(rootProject).get()
        assertEquals("root", properties.property("a", rootProject).get())

        rootProject.extraProperties.set("a", "non-root")
        assertEquals("root", properties.property("a", rootProject).get())
    }

    private fun testLoadingGradleProperty(
        configuredPropValue: Any?,
        expected: Any?,
        property: PropertiesBuildService.GradleProperty<*>
    ) {
        val project = buildProject()
        project.gradle.registerMinimalVariantImplementationFactoriesForTests()
        val properties = project.propertiesService.get()

        if (configuredPropValue != null) project.extraProperties.set(property.name, configuredPropValue)

        assertEquals(
            expected,
            when (property) {
                is PropertiesBuildService.BooleanGradleProperty -> properties.property(property, project).get()
                is PropertiesBuildService.StringGradleProperty -> properties.property(property, project).get()
                is PropertiesBuildService.NullableStringGradleProperty -> properties.property(property, project).orNull
                is PropertiesBuildService.IntGradleProperty -> properties.property(property, project).get()
                is PropertiesBuildService.NullableBooleanGradleProperty -> properties.property(property, project).orNull
                else -> error("Unexpected property type ${property::class}")
            }
        )
    }

    @Test
    fun testLoadingBooleanTrueString() {
        testLoadingGradleProperty(
            "TrUe",
            true,
            PropertiesBuildService.BooleanGradleProperty("some.prop", false)
        )
    }

    @Test
    fun testLoadingBooleanFalseString() {
        testLoadingGradleProperty(
            "FaLsE",
            false,
            PropertiesBuildService.BooleanGradleProperty("some.prop", true)
        )
    }

    @Test
    fun testLoadingBooleanDefaultValue() {
        testLoadingGradleProperty(
            null,
            false,
            PropertiesBuildService.BooleanGradleProperty("some.prop", false)
        )
    }

    @Test
    fun testNonBooleanFallbackToDefaultValue() {
        testLoadingGradleProperty(
            "Kodee!",
            false,
            PropertiesBuildService.BooleanGradleProperty("some.prop", false)
        )
    }

    @Test
    fun testLoadingTrueAsNullableBooleanValue() {
        testLoadingGradleProperty(
            "true",
            true,
            PropertiesBuildService.NullableBooleanGradleProperty("some.prop")
        )
    }

    @Test
    fun testLoadingFalseAsNullableBooleanValue() {
        testLoadingGradleProperty(
            "false",
            false,
            PropertiesBuildService.NullableBooleanGradleProperty("some.prop")
        )
    }

    @Test
    fun testLoadingIncorrectValueAsNullableBooleanValue() {
        testLoadingGradleProperty(
            "Kodee!",
            null,
            PropertiesBuildService.NullableBooleanGradleProperty("some.prop")
        )
    }

    @Test
    fun testLoadingNotConfiguredNullableBooleanValue() {
        testLoadingGradleProperty(
            null,
            null,
            PropertiesBuildService.NullableBooleanGradleProperty("some.prop")
        )
    }

    @Test
    fun testLoadingStringDefaultValue() {
        testLoadingGradleProperty(
            null,
            "Kodee!",
            PropertiesBuildService.StringGradleProperty("some.prop", "Kodee!"),
        )
    }

    @Test
    fun testLoadingStringValue() {
        testLoadingGradleProperty(
            "Happy",
            "Happy",
            PropertiesBuildService.StringGradleProperty("some.prop", "Kodee!"),
        )
    }

    @Test
    fun testLoadingNullableStringValue() {
        testLoadingGradleProperty(
            "Kodee!",
            "Kodee!",
            PropertiesBuildService.NullableStringGradleProperty("some.prop")
        )
    }

    @Test
    fun testLoadingNullableStringValueDefault() {
        testLoadingGradleProperty(
            null,
            null,
            PropertiesBuildService.NullableStringGradleProperty("some.prop")
        )
    }

    @Test
    fun testLoadingIntValue() {
        testLoadingGradleProperty(
            "2",
            2,
            PropertiesBuildService.IntGradleProperty("some.prop", 10)
        )
    }

    @Test
    fun testLoadingInvalidIntValue() {
        testLoadingGradleProperty(
            "Kodee!",
            10,
            PropertiesBuildService.IntGradleProperty("some.prop", 10)
        )
    }

    @Test
    fun testLoadingDefaultIntValue() {
        testLoadingGradleProperty(
            null,
            10,
            PropertiesBuildService.IntGradleProperty("some.prop", 10)
        )
    }
}
