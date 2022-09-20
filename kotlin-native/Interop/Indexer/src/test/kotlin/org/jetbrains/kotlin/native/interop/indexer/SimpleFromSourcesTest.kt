package org.jetbrains.kotlin.native.interop.indexer

import kotlin.contracts.ExperimentalContracts

@OptIn(ExperimentalContracts::class)
class SimpleFromSourcesTest : AbstractIndexerFromSourcesTest() {

    fun testFull() = doTestSuccessfulCInterop(false)
    fun testFullFModules() = doTestSuccessfulCInterop(true)

    fun testFilterA() = doTestSuccessfulCInterop(false)
    fun testFilterAFModules() = doTestSuccessfulCInterop(true)

    fun testFilterB() = doTestSuccessfulCInterop(false)
    fun testFilterBFModules() = doTestSuccessfulCInterop(true)

    fun testFilterC() = doTestSuccessfulCInterop(false)
    fun testFilterCFModules() = doTestSuccessfulCInterop(true)

    fun testFilterAB() = doTestSuccessfulCInterop(false)
    fun testFilterABFModules() = doTestSuccessfulCInterop(true)

    fun testFilterAC() = doTestSuccessfulCInterop(false)
    fun testFilterACFModules() = doTestSuccessfulCInterop(true)

    fun testFilterBC() = doTestSuccessfulCInterop(false)
    fun testFilterBCFModules() = doTestSuccessfulCInterop(true)

    fun testFilterABC() = doTestSuccessfulCInterop(false)
    fun testFilterABCFModules() = doTestSuccessfulCInterop(true)
}
