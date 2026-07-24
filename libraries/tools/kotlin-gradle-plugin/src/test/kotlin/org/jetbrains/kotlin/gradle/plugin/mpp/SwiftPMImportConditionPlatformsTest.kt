/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftPMDependency
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.conditionPlatforms
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SwiftPMImportConditionPlatformsTest {

    @Test
    fun `conditionPlatforms - implicit-only constraint is used as-is`() {
        assertEquals(
            setOf(SwiftPMDependency.Platform.iOS),
            conditionPlatforms(
                explicitPlatformConstraints = null,
                implicitPlatformConstraints = setOf(SwiftPMDependency.Platform.iOS),
                umbrellaPlatforms = setOf(SwiftPMDependency.Platform.iOS, SwiftPMDependency.Platform.macOS),
            )
        )
    }

    @Test
    fun `conditionPlatforms - constraint matching umbrella platforms yields no condition`() {
        // Producer and consumer targets match, so there's nothing to restrict — guards against over-constraining.
        assertNull(
            conditionPlatforms(
                explicitPlatformConstraints = null,
                implicitPlatformConstraints = setOf(SwiftPMDependency.Platform.iOS),
                umbrellaPlatforms = setOf(SwiftPMDependency.Platform.iOS),
            )
        )
    }

    @Test
    fun `conditionPlatforms - constraint covering all umbrella platforms yields no condition`() {
        // Producer supports more platforms than the consumer targets, so the constraint restricts nothing.
        assertNull(
            conditionPlatforms(
                explicitPlatformConstraints = null,
                implicitPlatformConstraints = setOf(SwiftPMDependency.Platform.iOS, SwiftPMDependency.Platform.macOS),
                umbrellaPlatforms = setOf(SwiftPMDependency.Platform.iOS),
            )
        )
    }

    @Test
    fun `conditionPlatforms - empty explicit set does not defeat the implicit constraint`() {
        // An explicit `platforms = emptySet()` (e.g. `product(name, platforms = emptySet())`) must be treated as unset.
        assertEquals(
            setOf(SwiftPMDependency.Platform.iOS),
            conditionPlatforms(
                explicitPlatformConstraints = emptySet(),
                implicitPlatformConstraints = setOf(SwiftPMDependency.Platform.iOS),
                umbrellaPlatforms = setOf(SwiftPMDependency.Platform.iOS, SwiftPMDependency.Platform.macOS),
            )
        )
    }

    @Test
    fun `conditionPlatforms - explicit constraint overrides the implicit one`() {
        assertEquals(
            setOf(SwiftPMDependency.Platform.iOS, SwiftPMDependency.Platform.macOS),
            conditionPlatforms(
                explicitPlatformConstraints = setOf(SwiftPMDependency.Platform.iOS, SwiftPMDependency.Platform.macOS),
                implicitPlatformConstraints = setOf(SwiftPMDependency.Platform.iOS),
                umbrellaPlatforms = setOf(
                    SwiftPMDependency.Platform.iOS,
                    SwiftPMDependency.Platform.macOS,
                    SwiftPMDependency.Platform.tvOS,
                ),
            )
        )
    }

    @Test
    fun `conditionPlatforms - explicit constraint wins even when it doesn't overlap the implicit one`() {
        assertEquals(
            setOf(SwiftPMDependency.Platform.macOS),
            conditionPlatforms(
                explicitPlatformConstraints = setOf(SwiftPMDependency.Platform.macOS),
                implicitPlatformConstraints = setOf(SwiftPMDependency.Platform.iOS),
                umbrellaPlatforms = setOf(SwiftPMDependency.Platform.iOS, SwiftPMDependency.Platform.macOS),
            )
        )
    }
}
