/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.compiler.plugins.kotlin

import org.junit.Test

class ControlFlowTransformTestsNoSource : AbstractControlFlowTransformTests() {
    override val sourceInformationEnabled: Boolean get() = false

    @Test
    fun testPublicFunctionAlwaysMarkedAsCall(): Unit = controlFlow(
        """
            @Composable
            fun Test() {
              A(a)
              A(b)
            }
        """,
        """
            @Composable
            fun Test(%composer: Composer?, %changed: Int) {
              if (isTraceInProgress()) {
                traceEventStart(<>)
              }
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)")
              if (%changed !== 0 || !%composer.skipping) {
                A(a, %composer, 0)
                A(b, %composer, 0)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(%composer, %changed or 0b0001)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
            }
        """
    )

    @Test
    fun testPrivateFunctionDoNotGetMarkedAsCall(): Unit = controlFlow(
        """
            @Composable
            private fun Test() {
              A(a)
              A(b)
            }
        """,
        """
            @Composable
            private fun Test(%composer: Composer?, %changed: Int) {
              if (isTraceInProgress()) {
                traceEventStart(<>)
              }
              %composer = %composer.startRestartGroup(<>)
              if (%changed !== 0 || !%composer.skipping) {
                A(a, %composer, 0)
                A(b, %composer, 0)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(%composer, %changed or 0b0001)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
            }
        """
    )

    @Test
    fun testCallingAWrapperComposable(): Unit = controlFlow(
        """
            @Composable
            fun Test() {
              W {
                A()
              }
            }
        """,
        """
            @Composable
            fun Test(%composer: Composer?, %changed: Int) {
              if (isTraceInProgress()) {
                traceEventStart(<>)
              }
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)")
              if (%changed !== 0 || !%composer.skipping) {
                W(ComposableSingletons%TestKt.lambda-1, %composer, 0b0110)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(%composer, %changed or 0b0001)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
            }
            internal object ComposableSingletons%TestKt {
              val lambda-1: Function2<Composer, Int, Unit> = composableLambdaInstance(<>, false) { %composer: Composer?, %changed: Int ->
                if (%changed and 0b1011 !== 0b0010 || !%composer.skipping) {
                  A(%composer, 0)
                } else {
                  %composer.skipToGroupEnd()
                }
              }
            }
        """
    )

    @Test
    fun testCallingAnInlineWrapperComposable(): Unit = controlFlow(
        """
            @Composable
            fun Test() {
              IW {
                A()
              }
            }
        """,
        """
            @Composable
            fun Test(%composer: Composer?, %changed: Int) {
              if (isTraceInProgress()) {
                traceEventStart(<>)
              }
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)")
              if (%changed !== 0 || !%composer.skipping) {
                IW({ %composer: Composer?, %changed: Int ->
                  if (%changed and 0b1011 !== 0b0010 || !%composer.skipping) {
                    A(%composer, 0)
                  } else {
                    %composer.skipToGroupEnd()
                  }
                }, %composer, 0)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(%composer, %changed or 0b0001)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
            }
        """
    )
}
