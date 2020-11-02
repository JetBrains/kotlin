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

class LambdaMemoizationTransformTests : ComposeIrTransformTest() {

    @Test // regression of b/162575428
    fun testComposableInAFunctionParameter(): Unit = verifyComposeIrTransform(
        """
            import androidx.compose.runtime.Composable

            @Composable
            fun Test(enabled: Boolean, content: @Composable () -> Unit = {
                    Display("%enabled")
                }
            ) {
                Wrap(content)
            }
        """.replace('%', '$'),
        """
            @Composable
            fun Test(enabled: Boolean, content: Function2<Composer<*>, Int, Unit>?, %composer: Composer<*>?, %changed: Int, %default: Int) {
              %composer.startRestartGroup(<>, "C(Test)P(1)<Wrap(c...>:Test.kt")
              val %dirty = %changed
              val content = content
              if (%default and 0b0001 !== 0) {
                %dirty = %dirty or 0b0110
              } else if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(enabled)) 0b0100 else 0b0010
              }
              if (%default and 0b0010 !== 0) {
                %dirty = %dirty or 0b00110000
              } else if (%changed and 0b01110000 === 0) {
                %dirty = %dirty or if (%composer.changed(content)) 0b00100000 else 0b00010000
              }
              if (%dirty and 0b01011011 xor 0b00010010 !== 0 || !%composer.skipping) {
                if (%default and 0b0010 !== 0) {
                  content = composableLambda(%composer, <>, true, "C<Displa...>:Test.kt") { %composer: Composer<*>?, %changed: Int ->
                    if (%changed and 0b1011 xor 0b0010 !== 0 || !%composer.skipping) {
                      Display("%enabled", %composer, 0)
                    } else {
                      %composer.skipToGroupEnd()
                    }
                  }
                }
                Wrap(content, %composer, 0b1110 and %dirty shr 0b0011)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer<*>?, %force: Int ->
                Test(enabled, content, %composer, %changed or 0b0001, %default)
              }
            }
        """,
        """
            import androidx.compose.runtime.Composable

            @Composable fun Display(text: String) { }
            @Composable fun Wrap(content: @Composable () -> Unit) { }
        """
    )

    @Test
    fun testComposabableLambdaInLocalDeclaration(): Unit = verifyComposeIrTransform(
        """
            import androidx.compose.runtime.Composable

            @Composable
            fun Test(enabled: Boolean) {
                val content: @Composable () -> Unit = {
                    Display("%enabled")
                }
                Wrap(content)
            }
        """.replace('%', '$'),
        """
            @Composable
            fun Test(enabled: Boolean, %composer: Composer<*>?, %changed: Int) {
              %composer.startRestartGroup(<>, "C(Test)<Wrap(c...>:Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(enabled)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 xor 0b0010 !== 0 || !%composer.skipping) {
                val content = composableLambda(%composer, <>, true, "C<Displa...>:Test.kt") { %composer: Composer<*>?, %changed: Int ->
                  if (%changed and 0b1011 xor 0b0010 !== 0 || !%composer.skipping) {
                    Display("%enabled", %composer, 0)
                  } else {
                    %composer.skipToGroupEnd()
                  }
                }
                Wrap(content, %composer, 0b0110)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer<*>?, %force: Int ->
                Test(enabled, %composer, %changed or 0b0001)
              }
            }
        """,
        """
            import androidx.compose.runtime.Composable

            @Composable fun Display(text: String) { }
            @Composable fun Wrap(content: @Composable () -> Unit) { }
        """
    )
}