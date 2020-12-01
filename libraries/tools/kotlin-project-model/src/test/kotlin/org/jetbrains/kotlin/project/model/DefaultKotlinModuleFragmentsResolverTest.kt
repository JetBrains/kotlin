/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.model

import org.junit.Assume.assumeTrue
import org.junit.Test
import kotlin.test.assertEquals

class DefaultKotlinModuleFragmentsResolverTest {
    val moduleFoo = simpleModule("foo")
    val moduleBar = simpleModule("bar")

    val fragmentResolver = DefaultKotlinModuleFragmentsResolver(MatchVariantsByExactAttributes())

    @Test
    fun testFragmentVisibility() {
        val expectedVisibleFragments = mapOf(
            "common" to setOf("commonMain"),
            "jvmAndJs" to setOf("commonMain", "jvmAndJsMain"),
            "jsAndLinux" to setOf("commonMain", "jsAndLinuxMain"),
            "jvm" to setOf("commonMain", "jvmAndJsMain", "jvmMain"),
            "js" to setOf("commonMain", "jvmAndJsMain", "jsAndLinuxMain", "jsMain"),
            "linux" to setOf("commonMain", "jsAndLinuxMain", "linuxMain")
        )

        moduleBar.fragments.forEach { consumingFragment ->
            val result = fragmentResolver.getChosenFragments(consumingFragment, moduleFoo)
            val expected = expectedVisibleFragments.getValue(consumingFragment.fragmentName.removeSuffix("Main").removeSuffix("Test"))
            assertEquals(expected, result.visibleFragments.map { it.fragmentName }.toSet())
        }
    }

    @Test
    fun testVisibilityWithMismatchedVariant() {
        // TODO this behavior replicates 1.3.x MPP where a mismatched variant gets ignored and only matched variants are intersected.
        //  This helps with non-published local native targets.
        //  Consider making it more strict when we have a solution to the original problem.
        val dependingModule = simpleModule("baz").apply {
            variant("linuxMain").variantAttributes.replace(KotlinNativeTargetAttribute, "notLinux")
        }
        assumeTrue(MatchVariantsByExactAttributes().getChosenVariant(dependingModule.variant("linuxMain"), moduleFoo) is NoVariantMatch)

        val (commonMainResult, jsAndLinuxResult) = listOf("commonMain", "jsAndLinuxMain").map {
            fragmentResolver.getChosenFragments(dependingModule.fragment(it), moduleFoo).visibleFragments.map { it.fragmentName }.toSet()
        }

        assertEquals(setOf("commonMain", "jvmAndJsMain"), commonMainResult)
        assertEquals(setOf("commonMain", "jvmAndJsMain", "jsAndLinuxMain", "jsMain"), jsAndLinuxResult)
    }
}