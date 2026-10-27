/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.optimizations

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class CallGraphHierarchyDispatchCacheTest {
    private val module = DataFlowIR.Module()
    private val callee = DataFlowIR.FunctionSymbol.Private(0, module, 0, 0, null, null)
    private val receiver = DataFlowIR.Type.Private(0, true, false, module, 0, null).apply {
        vtable.add(callee)
    }
    private val hierarchy = DataFlowIR.TypeHierarchy(arrayOf(receiver))

    private fun call() = DataFlowIR.Node.VtableCall(callee, receiver, 0, emptyList(), receiver, null)

    @Test
    fun opaqueTargetsAreRegisteredOncePerCache() {
        for (unfoldFactor in listOf(-1, 0)) {
            val roots = mutableListOf<DataFlowIR.FunctionSymbol>()
            repeat(2) {
                val resolve = createDispatchResolverForTests(hierarchy, unfoldFactor) { roots.add(it) }
                assertNull(resolve(call()))
                assertNull(resolve(call()))
                assertEquals(List(it + 1) { callee }, roots)
            }
        }
    }

    @Test
    fun concreteTargetsAreReusedWithoutRegisteringRoots() {
        val roots = mutableListOf<DataFlowIR.FunctionSymbol>()
        val resolve = createDispatchResolverForTests(hierarchy, 1) { roots.add(it) }
        val targets = resolve(call())
        assertEquals(listOf(callee), targets)
        assertSame(targets, resolve(call()))
        assertEquals(emptyList<DataFlowIR.FunctionSymbol>(), roots)
    }

    @Test
    fun emptyTargetsRemainDistinctFromOpaqueCalls() {
        val abstractReceiver = DataFlowIR.Type.Private(0, false, true, module, 0, null)
        val resolve = createDispatchResolverForTests(DataFlowIR.TypeHierarchy(arrayOf(abstractReceiver)), 1) {
            error("An abstract receiver has no targets")
        }
        val call = DataFlowIR.Node.VtableCall(callee, abstractReceiver, 0, emptyList(), abstractReceiver, null)
        val targets = resolve(call)
        assertEquals(emptyList<DataFlowIR.FunctionSymbol>(), targets)
        assertSame(targets, resolve(call))
    }

    @Test
    fun opaqueConcreteAndEmptyTargetsRemainIndependent() {
        val base = DataFlowIR.Type.Private(1, false, true, module, 1, null)
        receiver.superTypes.add(base)
        val sibling = DataFlowIR.Type.Private(2, true, false, module, 2, null).apply {
            superTypes.add(base)
            vtable.add(callee)
        }
        val emptyReceiver = DataFlowIR.Type.Private(3, false, true, module, 3, null)
        val roots = mutableListOf<DataFlowIR.FunctionSymbol>()
        val resolve = createDispatchResolverForTests(
                DataFlowIR.TypeHierarchy(arrayOf(receiver, base, sibling, emptyReceiver)), 1
        ) { roots.add(it) }
        val opaqueCall = DataFlowIR.Node.VtableCall(callee, base, 0, emptyList(), base, null)
        val emptyCall = DataFlowIR.Node.VtableCall(callee, emptyReceiver, 0, emptyList(), emptyReceiver, null)

        repeat(2) {
            assertNull(resolve(opaqueCall))
            assertEquals(listOf(callee), resolve(call()))
            assertEquals(emptyList<DataFlowIR.FunctionSymbol>(), resolve(emptyCall))
            assertEquals(listOf(callee, callee), roots)
        }
    }
}
