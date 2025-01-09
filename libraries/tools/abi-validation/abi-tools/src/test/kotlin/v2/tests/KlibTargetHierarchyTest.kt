/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abi.tools.legacy

import org.jetbrains.kotlin.abi.tools.api.v2.KlibTarget
import org.jetbrains.kotlin.abi.tools.v2.klib.TargetHierarchy
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class KlibTargetHierarchyTest {
    @Test
    fun testHierarchy() {
        assertContentEquals(listOf("linuxArm64", "linux", "native", "all"),
            hierarchyFrom("linuxArm64"))

        assertContentEquals(listOf("js", "all"),
            hierarchyFrom("js"))

        assertContentEquals(listOf("iosArm64", "ios", "apple", "native", "all"),
            hierarchyFrom("iosArm64"))

        assertContentEquals(listOf("androidNative", "native", "all"),
            hierarchyFrom("androidNative"))

        assertContentEquals(listOf("unknown"), hierarchyFrom("unknown"))
    }

    @Test
    fun testTargetsList() {
        assertEquals(setOf("linuxX64"), TargetHierarchy.targets("linuxX64"))
        assertEquals(setOf("macosX64", "macosArm64"), TargetHierarchy.targets("macos"))
        assertEquals(emptySet(), TargetHierarchy.targets("unknown"))
    }

    @Test
    fun testEveryMappedTargetIsWithinTheHierarchy() {
        KlibTarget.supportedKonanNames().forEach { underlyingTarget ->
            val name = KlibTarget.fromKonanTargetName(underlyingTarget).targetName
            assertNotNull(
                TargetHierarchy.parent(name),
                "Target $name.$underlyingTarget is missing from the hierarchy.")
        }
    }

    @Test
    fun testAllTargetsAreMapped() {
        val notMappedTargets = KonanTarget.predefinedTargets.keys.subtract(KlibTarget.supportedKonanNames())
        assertEquals(emptySet(), notMappedTargets,
            "Following targets are not mapped: $notMappedTargets")
    }

    private fun hierarchyFrom(groupOrTarget: String): List<String> {
        return buildList {
            var i = 0
            var group: String? = groupOrTarget
            while (group != null) {
                if (i > TargetHierarchy.hierarchyIndex.size) {
                    throw AssertionError("Cycle detected: $this")
                }
                add(group)
                group = TargetHierarchy.parent(group)
                i++
            }
        }
    }
}
