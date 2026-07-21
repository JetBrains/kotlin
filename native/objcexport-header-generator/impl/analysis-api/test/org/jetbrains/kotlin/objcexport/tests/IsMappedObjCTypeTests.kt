/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.tests

import org.jetbrains.kotlin.analysis.api.session.analyze
import org.jetbrains.kotlin.analysis.api.session.useSiteSession
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.defaultType
import org.jetbrains.kotlin.export.test.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.export.test.getClassOrFail
import org.jetbrains.kotlin.export.test.getFunctionOrFail
import org.jetbrains.kotlin.objcexport.isMappedObjCType
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertFalse
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import org.junit.jupiter.api.Test

class IsMappedObjCTypeTests(
    private val inlineSourceCodeAnalysis: InlineSourceCodeAnalysis,
) {
    @Test
    fun `test - basic class`() {
        val file = inlineSourceCodeAnalysis.createKtFile("class Foo".trimMargin())
        analyze(file) {
            val session = useSiteSession
            assertFalse(session.isMappedObjCType(file.getClassOrFail("Foo", session).defaultType))
        }
    }

    @Test
    fun `test - list`() {
        val file = inlineSourceCodeAnalysis.createKtFile("fun List<Any>.listFoo() = Unit")
        analyze(file) {
            val session = useSiteSession
            assertTrue(session.isMappedObjCType(file.getFunctionOrFail("listFoo", session).receiverType))
        }
    }
}

private val KaNamedFunctionSymbol.receiverType: KaType get() = receiverParameter?.returnType ?: error("$name doesn't have receiver")
