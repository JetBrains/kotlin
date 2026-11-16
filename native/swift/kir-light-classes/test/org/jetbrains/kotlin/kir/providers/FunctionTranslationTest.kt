/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kir.providers

import org.jetbrains.kotlin.kir.KirFunction
import org.jetbrains.kotlin.kir.KirVisibility
import org.jetbrains.kotlin.kir.providers.source.SirSource
import org.jetbrains.kotlin.kir.providers.support.functionsNamed
import org.jetbrains.kotlin.kir.providers.support.translate
import org.jetbrains.kotlin.sir.SirFunction
import org.jetbrains.kotlin.sir.SirUnsupportedType
import org.jetbrains.kotlin.sir.SirVisibility
import org.jetbrains.kotlin.sir.builder.buildFunction
import org.jetbrains.kotlin.sir.builder.buildModule
import org.jetbrains.kotlin.sir.builder.buildStruct
import org.jetbrains.kotlin.sir.util.addChild
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class FunctionTranslationTest {
    private fun sirFunction(functionName: String, functionVisibility: SirVisibility = SirVisibility.PUBLIC): SirFunction =
        buildFunction {
            name = functionName
            returnType = SirUnsupportedType
            visibility = functionVisibility
        }

    @Test
    fun `top-level functions are translated in order`() {
        val sirModule = buildModule { name = "Lib" }.apply {
            addChild { sirFunction("foo") }
            addChild { sirFunction("bar") }
        }

        sirModule.translate { kirModule ->
            assertEquals(listOf("foo", "bar"), kirModule.declarations.map { (it as KirFunction).name })
        }
    }

    @Test
    fun `declarations other than functions are not translated`() {
        val sirModule = buildModule { name = "Lib" }.apply {
            addChild { sirFunction("foo") }
            addChild { buildStruct { name = "S" } }
        }

        sirModule.translate { kirModule ->
            assertEquals(1, kirModule.declarations.size)
            assertEquals(1, kirModule.functionsNamed("foo").size)
        }
    }

    @Test
    fun `functions not visible outside of the module are not translated`() {
        val sirModule = buildModule { name = "Lib" }.apply {
            addChild { sirFunction("visible") }
            addChild { sirFunction("packaged", SirVisibility.PACKAGE) }
            addChild { sirFunction("hidden", SirVisibility.INTERNAL) }
            addChild { sirFunction("secret", SirVisibility.PRIVATE) }
        }

        sirModule.translate { kirModule ->
            assertEquals(listOf("visible"), kirModule.declarations.map { (it as KirFunction).name })
        }
    }

    @Test
    fun `translated function has the module as a parent and keeps its source`() {
        val sirFunction = sirFunction("foo")
        val sirModule = buildModule { name = "Lib" }.apply { addChild { sirFunction } }

        sirModule.translate { kirModule ->
            val function = kirModule.functionsNamed("foo").single()
            assertSame(kirModule, function.parent)
            assertSame(sirFunction, (function.origin as SirSource).declaration)
            assertEquals(KirVisibility.PUBLIC, function.visibility)
            assertNull(function.body)
            assertTrue(function.bridges.isEmpty())
        }
    }

    @Test
    fun `translating the same declaration twice returns the same node`() {
        val sirFunction = sirFunction("foo")
        val sirModule = buildModule { name = "Lib" }.apply { addChild { sirFunction } }

        sirModule.translate {
            val first = with(sirFunction) { toKir() }
            val second = with(sirFunction) { toKir() }
            assertSame(first, second)
        }
    }
}
