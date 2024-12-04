/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments

import org.jdom.Element
import org.jetbrains.kotlin.config.KotlinFacetSettings
import org.jetbrains.kotlin.config.deserializeFacetSettings
import org.jetbrains.kotlin.config.serializeFacetSettings
import org.junit.Test
import kotlin.test.assertEquals

class FacetSettingsSerializationTest {

    @Test
    fun `test - module dependencies`() {
        val source = KotlinFacetSettings().apply {
            implementedModuleNames = listOf("implementedModule1", "implementedModule2")
            dependsOnModuleNames = listOf("dependsOnModule1", "dependsOnModule2")
            additionalVisibleModuleNames = setOf("friend1", "friend2")
        }

        val deserialized = serializeAndDeserialize(source)
        assertEquals(source.implementedModuleNames, deserialized.implementedModuleNames)
        assertEquals(source.dependsOnModuleNames, deserialized.dependsOnModuleNames)
        assertEquals(source.additionalVisibleModuleNames, deserialized.additionalVisibleModuleNames)
    }

    private fun serializeAndDeserialize(settings: KotlinFacetSettings): KotlinFacetSettings {
        val element = Element("settings")
        settings.serializeFacetSettings(element)
        return deserializeFacetSettings(element)
    }
}
