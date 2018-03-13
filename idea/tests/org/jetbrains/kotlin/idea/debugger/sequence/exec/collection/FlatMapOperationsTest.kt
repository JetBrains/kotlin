// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.kotlin.idea.debugger.sequence.exec.collection

import org.jetbrains.kotlin.idea.debugger.sequence.exec.collection.CollectionTestCase
import org.junit.Ignore

/**
 * @author Vitaliy.Bibaev
 */
@Ignore("enable to test collection transform operations")
class FlatMapOperationsTest : CollectionTestCase("flatMap") {
  fun testFlatMap() {
    doTestWithResult()
  }
}