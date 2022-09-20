package org.jetbrains.kotlin.native.interop.indexer

import kotlin.contracts.ExperimentalContracts

@OptIn(ExperimentalContracts::class)
class FrameworkFromSourcesTest : AbstractIndexerFromSourcesTest() {

    fun testFull() = doTestSuccessfulCInterop(false)
    fun testFullFModules() = doTestSuccessfulCInterop(true)

    fun testFilterPod1() = doTestSuccessfulCInterop(false)
    fun testFilterPod1FModules() = doTestSuccessfulCInterop(true)

    fun testFilterPod1Umbrella() = doTestSuccessfulCInterop(false)
    fun testFilterPod1UmbrellaFModules() = doTestSuccessfulCInterop(true)

    fun testFilterPod1A() = doTestSuccessfulCInterop(false)
    fun testFilterPod1AFModules() = doTestSuccessfulCInterop(true)

    fun testFilterPod1UmbrellaPod1A() = doTestSuccessfulCInterop(false)
    fun testFilterPod1UmbrellaPod1AFModules() = doTestSuccessfulCInterop(true)
}
