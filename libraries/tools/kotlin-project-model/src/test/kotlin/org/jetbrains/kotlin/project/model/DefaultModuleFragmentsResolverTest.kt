/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.model

import org.junit.Assume.assumeTrue
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class DefaultModuleFragmentsResolverTest {
    val bundleFoo = simpleModuleBundle("foo")
    val bundleBar = simpleModuleBundle("bar")

    val fragmentResolver = DefaultModuleFragmentsResolver(MatchVariantsByExactAttributes())

    @Test
    fun testFragmentVisibility() {
        val moduleFooMain = bundleFoo.main
        val moduleBarMain = bundleBar.main

        val expectedVisibleFragments = mapOf(
            "common" to setOf("common"),
            "jvmAndJs" to setOf("common", "jvmAndJs"),
            "jsAndLinux" to setOf("common", "jsAndLinux"),
            "jvm" to setOf("common", "jvmAndJs", "jvm"),
            "js" to setOf("common", "jvmAndJs", "jsAndLinux", "js"),
            "linux" to setOf("common", "jsAndLinux", "linux")
        )

        moduleBarMain.fragments.forEach { consumingFragment ->
            val result = fragmentResolver.getChosenFragments(consumingFragment, moduleFooMain)
            assertTrue(result is FragmentResolution.ChosenFragments)
            val expected = expectedVisibleFragments.getValue(consumingFragment.fragmentName)
            assertEquals(expected, result.visibleFragments.map { it.fragmentName }.toSet())
        }
    }

    @Test
    fun testVisibilityWithMismatchedVariant() {
        // TODO this behavior replicates 1.3.x MPP where a mismatched variant gets ignored and only matched variants are intersected.
        //  This helps with non-published local native targets.
        //  Consider making it more strict when we have a solution to the original problem.
        val dependingModule = simpleModuleBundle("baz").main.apply {
            variant("linux").variantAttributes.replace(KotlinNativeTargetAttribute, "notLinux")
        }
        val moduleFooMain = bundleFoo.main
        val variantResolution = MatchVariantsByExactAttributes().getChosenVariant(dependingModule.variant("linux"), moduleFooMain)
        assumeTrue(variantResolution is VariantResolution.NoVariantMatch)

        val (commonMainResult, jsAndLinuxResult) = listOf("common", "jsAndLinux").map {
            val chosenFragments = fragmentResolver.getChosenFragments(dependingModule.fragment(it), moduleFooMain)
            assertTrue(chosenFragments is FragmentResolution.ChosenFragments)
            chosenFragments.visibleFragments.map { it.fragmentName }.toSet()
        }

        assertEquals(setOf("common", "jvmAndJs"), commonMainResult)
        assertEquals(setOf("common", "jvmAndJs", "jsAndLinux", "js"), jsAndLinuxResult)
    }
}