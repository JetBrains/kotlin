/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DomainNotationTest {
    @Test
    fun `all domains`() {
        assertEquals("*", Domain.entries.toSet().toArgumentString())
        assertEquals(Domain.entries.toSet(), Domain.fromArgumentString("*"))
    }

    @Test
    fun `none notation`() {
        assertEquals("<none>", emptyList<Domain>().toArgumentString())
        assertEquals(emptySet(), Domain.fromArgumentString("<none>"))
    }

    @Test
    fun `empty string`() {
        assertNull(Domain.fromArgumentString(""))
    }

    @Test
    fun `default notation`() {
        assertEquals("Compiler;Gradle", setOf(Domain.Compiler, Domain.Gradle).toArgumentString())
        assertEquals(setOf(Domain.Compiler, Domain.Gradle), Domain.fromArgumentString("Compiler;Gradle"))
    }
}