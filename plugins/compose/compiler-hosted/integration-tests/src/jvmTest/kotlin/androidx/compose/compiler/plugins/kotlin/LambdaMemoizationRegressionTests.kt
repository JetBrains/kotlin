/*
 * Copyright 2021 The Android Open Source Project
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

/**
 * This test merely ensures that code gen changes are evaluated against potentially
 * breaking Android Studio compose debugger integration, see change id
 * I63ce10791fc3795a568f5f09ca6a24e801f5e3da
 *
 * The Android Studio debugger searches for `ComposableSingletons` classes by name.
 * Any changes to the naming scheme have to be reflected in the Android Studio code.
 */
class LambdaMemoizationRegressionTests(useFir: Boolean) : AbstractIrTransformTest(useFir) {
    @Test
    fun testNestedComposableSingletonsClass() = verifyGoldenComposeIrTransform(
        """
            import androidx.compose.runtime.Composable

            class A {
                val x = @Composable {}
            }
        """
    )

    @Test
    fun testNestedComposableSingletonsClass2() = verifyGoldenComposeIrTransform(
        """
            import androidx.compose.runtime.Composable

            class A {
                class B {
                    val x = @Composable {}
                }
            }
        """
    )

    @Test
    fun testJvmNameComposableSingletons() = verifyGoldenComposeIrTransform(
        """
            @file:JvmName("A")
            import androidx.compose.runtime.Composable

            val x = @Composable {}
        """
    )
}
