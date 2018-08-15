// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.kotlin.idea.debugger.sequence.psi

import com.intellij.debugger.streams.trace.impl.handler.type.GenericType
import org.jetbrains.kotlin.idea.debugger.sequence.KotlinPsiChainBuilderTestCase

abstract class TypedChainTestCase(relativePath: String) : KotlinPsiChainBuilderTestCase(relativePath) {

    protected fun doTest(
        producerAfterType: GenericType,
        vararg intermediateAfterTypes: GenericType
    ) {
        val elementAtCaret = configureAndGetElementAtCaret()
        assertNotNull(elementAtCaret)
        val chains = chainBuilder.build(elementAtCaret)
        assertFalse(chains.isEmpty())
        val chain = chains[0]
        val intermediateCalls = chain.intermediateCalls
        assertEquals(intermediateAfterTypes.size, intermediateCalls.size)
        assertEquals(producerAfterType, chain.qualifierExpression.typeAfter)

        if (intermediateAfterTypes.isNotEmpty()) {
            assertEquals(producerAfterType, intermediateCalls[0].typeBefore)
            for (i in 0 until intermediateAfterTypes.size - 1) {
                assertEquals(intermediateAfterTypes[i], intermediateCalls[i].typeAfter)
                assertEquals(intermediateAfterTypes[i], intermediateCalls[i + 1].typeBefore)
            }

            val lastAfterType = intermediateAfterTypes[intermediateAfterTypes.size - 1]
            assertEquals(lastAfterType, chain.terminationCall.typeBefore)
            val lastCall = intermediateCalls[intermediateCalls.size - 1]
            assertEquals(lastAfterType, lastCall.typeAfter)
        } else {
            assertEquals(producerAfterType, chain.terminationCall.typeBefore)
        }
    }

    override fun doTest() {
        fail("Use doTest(producerAfterType, vararg intermediateAfterTypes)")
    }
}