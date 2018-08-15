// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.kotlin.idea.debugger.sequence.psi.java

import org.jetbrains.kotlin.idea.debugger.sequence.lib.java.JavaStandardLibrarySupportProvider
import org.jetbrains.kotlin.idea.debugger.sequence.psi.TypedChainTestCase
import org.jetbrains.kotlin.idea.debugger.sequence.trace.dsl.KotlinTypes.DOUBLE
import org.jetbrains.kotlin.idea.debugger.sequence.trace.dsl.KotlinTypes.INT
import org.jetbrains.kotlin.idea.debugger.sequence.trace.dsl.KotlinTypes.LONG
import org.jetbrains.kotlin.idea.debugger.sequence.trace.dsl.KotlinTypes.NULLABLE_ANY
import com.intellij.debugger.streams.wrapper.StreamChainBuilder

/**
 * @author Vitaliy.Bibaev
 */
class TypedJavaChainTest : TypedChainTestCase("streams/positive/types") {
  override val kotlinChainBuilder: StreamChainBuilder = JavaStandardLibrarySupportProvider().chainBuilder

  fun testOneCall() = doTest(NULLABLE_ANY)
  fun testMapToSame() = doTest(NULLABLE_ANY, NULLABLE_ANY)
  fun testPrimitiveOneCall() = doTest(INT)
  fun testPrimitiveMapToSame() = doTest(LONG, LONG)

  fun testMapToPrimitive() = doTest(NULLABLE_ANY, DOUBLE)
  fun testMapToObj() = doTest(DOUBLE, NULLABLE_ANY)
  fun testMapPrimitiveToPrimitive() = doTest(LONG, INT)

  fun testFewTransitions() = doTest(NULLABLE_ANY, INT, NULLABLE_ANY, LONG, DOUBLE)
}
