// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.kotlin.idea.debugger.sequence.exec.sequence

/**
 * @author Vitaliy.Bibaev
 */
class DistinctOperationsTest : OperationsTestCase("distinct") {
  fun testDistinct() = doTestWithResult()
  fun testDistinctObjects() = doTestWithResult()

  fun testDistinctBy() = doTestWithResult()
  fun testDistinctByNullableElement() = doTestWithResult()
  fun testDistinctByNullableKey() = doTestWithResult()
  fun testDistinctByNullableKeyAndElement() = doTestWithResult()
  fun testDistinctByBigPrimitives() = doTestWithResult()
}