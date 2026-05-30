/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abi.tools.impl

import org.jetbrains.kotlin.abi.tools.KlibTarget
import org.jetbrains.kotlin.abi.tools.impl.klib.TargetHierarchy
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class KlibTargetHierarchyTest {
    @Test
    fun testHierarchy() {
        assertContentEquals(
            ["linuxArm64", "linux", "native", "all"],
            hierarchyFrom("linuxArm64"))

        assertContentEquals(
            ["js", "all"],
            hierarchyFrom("js"))

        assertContentEquals(
            ["iosArm64", "ios", "apple", "native", "all"],
            hierarchyFrom("iosArm64"))

        assertContentEquals(
            ["androidNative", "native", "all"],
            hierarchyFrom("androidNative"))

        assertContentEquals(["unknown"], hierarchyFrom("unknown"))
    }

    @Test
    fun testTargetsList() {
        assertEquals(["linuxX64"], TargetHierarchy.targets("linuxX64"))
        assertEquals(["macosX64", "macosArm64"], TargetHierarchy.targets("macos"))
        assertEquals([], TargetHierarchy.targets("unknown"))
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
        assertEquals(
            [], notMappedTargets,
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
