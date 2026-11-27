/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.optimizations

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * A vtable call and an itable call whose interface id is 0 used to build the same [VirtualDispatchKey]
 * (the vtable side stored a sentinel 0). Before GHA, [ClassLayoutBuilder.classId] for an interface is a hash
 * that can be 0, so the two calls must not share a hub.
 */
class VirtualDispatchKeyTest {
    @Test
    fun vtableCallAndItableCallWithInterfaceIdZeroDoNotShareHub() {
        val receiver = receiverType(index = 7)
        val vtable = vtableCall(receiver, slot = 3)
        val itable = itableCall(receiver, interfaceId = 0, slot = 3)

        val vtableKey = key(vtable)
        val itableKey = key(itable)
        assertNotEquals(vtableKey, itableKey)

        // virtualDispatchHubs is a getOrPut on this key, so distinct keys are distinct hubs.
        val hubs = mutableMapOf<DevirtualizationAnalysis.DevirtualizationAnalysisImpl.VirtualDispatchKey, String>()
        assertEquals("vtable", hubs.getOrPut(vtableKey) { "vtable" })
        assertEquals("itable", hubs.getOrPut(itableKey) { "itable" })
        assertEquals(2, hubs.size)
    }

    @Test
    fun identicalVtableCallsStillShareHub() {
        val receiver = receiverType(index = 7)
        val first = key(vtableCall(receiver, slot = 3))
        val second = key(vtableCall(receiver, slot = 3))
        assertEquals(first, second)
    }

    @Test
    fun itableCallsWithInterfaceIdZeroStillShareHub() {
        val receiver = receiverType(index = 7)
        val first = key(itableCall(receiver, interfaceId = 0, slot = 3))
        val second = key(itableCall(receiver, interfaceId = 0, slot = 3))
        assertEquals(first, second)
    }

    private fun key(node: DataFlowIR.Node.VirtualCall) =
            DevirtualizationAnalysis.DevirtualizationAnalysisImpl.VirtualDispatchKey.create(node)

    private fun receiverType(index: Int): DataFlowIR.Type =
            DataFlowIR.Type.Private(
                    index = index,
                    isFinal = false,
                    isAbstract = false,
                    module = DataFlowIR.Module(),
                    symbolTableIndex = index,
                    irClass = null,
            )

    private fun callee(): DataFlowIR.FunctionSymbol =
            DataFlowIR.FunctionSymbol.Private(
                    index = 0,
                    module = DataFlowIR.Module(),
                    symbolTableIndex = 0,
                    attributes = 0,
                    irDeclaration = null,
                    bridgeTarget = null,
            )

    private fun vtableCall(receiver: DataFlowIR.Type, slot: Int) =
            DataFlowIR.Node.VtableCall(
                    callee(),
                    receiver,
                    slot,
                    emptyList(),
                    receiver,
                    null,
            )

    private fun itableCall(receiver: DataFlowIR.Type, interfaceId: Int, slot: Int) =
            DataFlowIR.Node.ItableCall(
                    callee(),
                    receiver,
                    interfaceId,
                    slot,
                    emptyList(),
                    receiver,
                    null,
            )
}
