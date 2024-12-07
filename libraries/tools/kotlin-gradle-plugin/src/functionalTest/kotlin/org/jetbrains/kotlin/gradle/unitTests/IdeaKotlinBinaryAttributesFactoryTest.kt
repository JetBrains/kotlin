/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.jetbrains.kotlin.gradle.plugin.ide.IdeaKotlinBinaryAttributes
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.utils.named
import kotlin.test.Test
import kotlin.test.assertEquals

class IdeaKotlinBinaryAttributesFactoryTest {

    @Test
    fun `test - sample 0`() {
        val project = buildProject()

        val dummyConfiguration = project.configurations.create("dummy")
        val container = dummyConfiguration.attributes
        container.attributeProvider(
            Usage.USAGE_ATTRIBUTE,
            project.provider { project.objects.named(Usage.JAVA_API) }
        )
        container.attributeProvider(
            Category.CATEGORY_ATTRIBUTE,
            project.provider { project.objects.named(Category.DOCUMENTATION) }
        )

        assertEquals(
            mapOf(
                "org.gradle.usage" to "java-api",
                "org.gradle.category" to "documentation"
            ), IdeaKotlinBinaryAttributes(container)
        )
    }
}
