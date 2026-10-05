/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven

import java.util.function.Function
import kotlin.test.*

class CiEnvironmentTest {

    private val noValues = Function<String, String?> { null }

    private fun valueOf(name: String) = Function<String, String?> { if (it == name) "true" else null }

    @Test
    fun `nothing is detected when no marker is present`() {
        assertNull(CiEnvironment.detectedCiMarker(noValues, noValues))
    }

    @Test
    fun `marker is detected via environment variable`() {
        assertEquals("TEAMCITY_VERSION", CiEnvironment.detectedCiMarker(valueOf("TEAMCITY_VERSION"), noValues))
    }

    @Test
    fun `marker is detected via system property`() {
        assertEquals("GITHUB_ACTIONS", CiEnvironment.detectedCiMarker(noValues, valueOf("GITHUB_ACTIONS")))
    }
}
