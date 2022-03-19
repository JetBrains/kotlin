/*
 * Copyright 2019 The Android Open Source Project
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

class SanityCheckCodegenTests : AbstractCodegenTest() {

    fun testCallAbstractSuperWithTypeParameters() = ensureSetup {
        testCompile(
            """
                abstract class AbstractB<Type>(d: Type) : AbstractA<Int, Type>(d) {
                    override fun test(key: Int): Type {
                        return super.test(key)
                    }
                }
                abstract class AbstractA<Type1, Type2>(var d: Type2) {
                    open fun test(key: Type1): Type2 = d
                }
        """
        )
    }

    // Regression test, because we didn't have a test to catch a breakage introduced by
    // https://github.com/JetBrains/kotlin/commit/ae608ea67fc589c4472657dc0317e97cb67dd158
    fun testNothings() = ensureSetup {
        testCompile(
            """
                import androidx.compose.runtime.Composable

                @Composable
                fun NothingToUnit(): Unit {
                    return error("")
                }

                @Composable
                fun NothingToNothing(): Nothing {
                    return error("")
                }

                @Composable
                fun NullableNothing(condition: Boolean): Nothing? {
                    return (if(condition) error("") else null)
                }
        """
        )
    }

    // Regression test for b/222979253
    fun testLabeledLambda() = ensureSetup {
        testCompile(
            """
                import androidx.compose.runtime.Composable

                @Composable
                fun test(): Unit {
                    Box box@{}
                }

                @Composable
                fun Box(content: @Composable () -> Unit) {}
        """
        )
    }
}
