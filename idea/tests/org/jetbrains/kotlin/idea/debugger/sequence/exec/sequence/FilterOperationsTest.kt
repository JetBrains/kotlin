// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.debugger.streams.kotlin.exec.sequence

class FilterOperationsTest : OperationsTestCase("filter") {
    fun testFilter() = doTestWithResult()
    fun testFilterNot() = doTestWithResult()
    fun testFilterIndexed() = doTestWithResult()
    fun testFilterIsInstance() = doTestWithoutResult()

    fun testDrop() = doTestWithResult()
    fun testDropWhile() = doTestWithoutResult()

    fun testMinus() = doTestWithResult()
    fun testMinusElement() = doTestWithResult()

    fun testTake() = doTestWithResult()
    fun testTakeWhile() = doTestWithoutResult()
}