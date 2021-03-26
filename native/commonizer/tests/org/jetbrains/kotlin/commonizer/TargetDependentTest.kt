/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer

import org.junit.Test
import kotlin.test.assertEquals

class TargetDependentTest {
    @Test
    fun toMap() {
        val targetDependent = TargetDependent(listOf(LeafCommonizerTarget("a"), LeafCommonizerTarget("b"))) {
            (it as LeafCommonizerTarget).name
        }

        assertEquals(
            mapOf(LeafCommonizerTarget("a") as CommonizerTarget to "a", LeafCommonizerTarget("b") as CommonizerTarget to "b"),
            targetDependent.toMap()
        )
    }

    @Test
    fun `create from map`() {
        val map = mapOf(LeafCommonizerTarget("a") to "a", LeafCommonizerTarget("b") to "b")
        val targetDependent = map.toTargetDependent()

        assertEquals(
            listOf(LeafCommonizerTarget("a"), LeafCommonizerTarget("b")),
            targetDependent.targets
        )

        @Suppress("useless_cast")
        assertEquals(
            map.mapKeys { (k, _) -> k as CommonizerTarget }, targetDependent.toMap(),
        )
    }
}
