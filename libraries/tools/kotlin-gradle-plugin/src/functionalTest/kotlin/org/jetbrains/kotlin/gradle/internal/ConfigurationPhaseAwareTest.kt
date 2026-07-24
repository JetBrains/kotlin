/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.internal

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class ConfigurationPhaseAwareTest {

    @Test
    fun `LegacyProperty - primitive property - expect modifications are reflected in the Gradle Property`() {
        val project = ProjectBuilder.builder().build()

        val data = FooData(project.objects, project.providers)

        assertEquals("1", data.stringProp.orNull)

        data.string = "2"

        assertEquals("2", data.stringProp.orNull)
    }

    @Test
    fun `LegacyProperty - primitive property - expect exception if already configured`() {
        val project = ProjectBuilder.builder().build()

        val data = FooData(project.objects, project.providers)

        data.requireConfigured()

        assertThrows<IllegalStateException>("Configuration already finalized for previous property values") {
            data.string = "1"
        }
    }

    @Test
    fun `LegacyProperty - list property - expect modifications are reflected in the Gradle Property`() {
        val project = ProjectBuilder.builder().build()

        val data = FooData(project.objects, project.providers)

        assertEquals("[]", data.listProp.orNull?.toString())

        data.list.add(1)
        assertEquals("[1]", data.listProp.orNull?.toString())

        data.list.remove(1)
        assertEquals("[]", data.listProp.orNull?.toString())

        data.list = mutableListOf(99)
        assertEquals("[99]", data.listProp.orNull?.toString())

        data.list.remove(99)
        assertEquals("[]", data.listProp.orNull?.toString())
    }

    @Test
    fun `LegacyProperty - list property - expect no exception if already configured`() {
        val project = ProjectBuilder.builder().build()

        val data = FooData(project.objects, project.providers)

        data.requireConfigured()

        assertThrows<IllegalStateException>("Configuration already finalized for previous property values") {
            data.list = mutableListOf(1)
        }
    }

    private class FooData(
        objects: ObjectFactory,
        providers: ProviderFactory,
    ) : ConfigurationPhaseAware<Unit>() {
        val stringProp = objects.property(String::class.java)
            .convention("1")
        var string by LegacyProperty(stringProp)

        var list: MutableList<Int> by @Suppress("DEPRECATION") Property(mutableListOf())
        val listProp = objects.listProperty(Int::class.java)
            .convention(providers.provider { list })

        // the result is not relevant for this test
        override fun finalizeConfiguration(): Unit = Unit
    }
}
