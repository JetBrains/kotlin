// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.kotlin.idea.debugger.sequence.exec.sequence

/**
 * @author Vitaliy.Bibaev
 */
class AppendOperationsTest : OperationsTestCase("append") {
  fun testPlusSingle() = doTestWithResult()
  fun testPlusArray() = doTestWithResult()
  fun testPlusSequence() = doTestWithResult()

  fun testPlusElement() = doTestWithResult()
}