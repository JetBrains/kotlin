/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.Named
import org.gradle.api.attributes.Attribute
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.containsMultiplatformAttributes
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import kotlin.test.*

abstract class BaseAttributeTest<T>(private val attribute: Attribute<T>) {
    protected fun assertNoAttribute(
        target: KotlinTarget,
        usages: Set<KotlinUsageContext> = target.usages,
    ) {
        usages.ifEmpty { fail("Expected at least one 'usage'") }
        for (usage in usages) {
            usage.attributes.containsMultiplatformAttributes
            assertNull(
                usage.attributes.getAttribute(attribute),
                "Expected no jvm environment attribute to be set on usage '$usage"
            )
        }
    }

    protected fun assertAttributeEquals(
        target: KotlinTarget,
        value: T,
        usages: Set<KotlinUsageContext> = target.usages,
    ) {
        usages.ifEmpty { fail("Expected at least one 'usage'") }
        for (usage in usages) {
            assertTrue(usage.attributes.containsMultiplatformAttributes)
            assertEquals(
                value,
                usage.attributes.getAttribute(attribute),
                usage.name,
            )
        }
    }

    protected val KotlinTarget.usages: Set<KotlinUsageContext>
        get() {
            val components = internal.kotlinComponents
            return components.flatMap { (it as InternalKotlinTargetComponent).usages }.toSet()
        }
}

abstract class BaseNamedAttributeTest<T : Named>(
    attribute: Attribute<T>,
    private val clazz: Class<T>,
) : BaseAttributeTest<T>(attribute) {
    protected fun assertAttributeEquals(
        target: KotlinTarget,
        value: String,
        usages: Set<KotlinUsageContext> = target.usages,
    ) {
        assertAttributeEquals(
            target,
            target.project.objects.named(clazz, value) as T,
            usages,
        )
    }
}