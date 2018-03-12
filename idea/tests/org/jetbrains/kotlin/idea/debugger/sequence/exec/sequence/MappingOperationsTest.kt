// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.debugger.streams.kotlin.exec.sequence

/**
 * @author Vitaliy.Bibaev
 */
class MappingOperationsTest : OperationsTestCase("map") {
  fun testMap() = doTestWithResult()
  fun testMapNotNull() = doTestWithResult()
  fun testMapIndexed() = doTestWithResult()
  fun testWithIndex() = doTestWithoutResult()
}