/*
 * Copyright 2023 The Android Open Source Project
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

class FunctionalInterfaceExtensionReceiverTransformTests : AbstractControlFlowTransformTests() {
    @Test
    fun testFunctionalInterfaceWithExtensionReceiverTransformation() {
        verifyComposeIrTransform(
            source = """
                import androidx.compose.runtime.*
                fun interface TestContent {
                    @Composable
                    fun String.Content()
                }
                @Composable
                fun Test(content: TestContent) {
                    with(content) {
                        "".Content()
                    }
                }

                @Composable
                fun CallTest() {
                    Test { this.length }
                }
            """.trimIndent(),
            expectedTransformed = """
            interface TestContent {
              @Composable
              abstract fun String.Content(%composer: Composer?, %changed: Int)
            }
            @Composable
            @ComposableInferredTarget(scheme = "[0[0]]")
            fun Test(content: TestContent, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)*<Conten...>:Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(content)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %changed, -1, <>)
                }
                with(content) {
                  %this%with.Content(%composer, 0b0110)
                }
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(content, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
            @Composable
            fun CallTest(%composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(CallTest)<Test>:Test.kt")
              if (%changed !== 0 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %changed, -1, <>)
                }
                Test(class <no name provided> : TestContent {
                  @Composable
                  override fun Content(%this%Test: String, %composer: Composer?, %changed: Int) {
                    %composer = %composer.startRestartGroup(<>)
                    sourceInformation(%composer, "C(Content):Test.kt")
                    val %dirty = %changed
                    if (%changed and 0b1110 === 0) {
                      %dirty = %dirty or if (%composer.changed(%this%Test)) 0b0100 else 0b0010
                    }
                    if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                      if (isTraceInProgress()) {
                        traceEventStart(<>, %changed, -1, <>)
                      }
                      %this%Test.length
                      if (isTraceInProgress()) {
                        traceEventEnd()
                      }
                    } else {
                      %composer.skipToGroupEnd()
                    }
                    val tmp0_rcvr = <this>
                    %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                      tmp0_rcvr.Content(%this%Test, %composer, updateChangedFlags(%changed or 0b0001))
                    }
                  }
                }
                <no name provided>(), %composer, 0)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                CallTest(%composer, updateChangedFlags(%changed or 0b0001))
              }
            }
            """.trimIndent()
        )
    }
}