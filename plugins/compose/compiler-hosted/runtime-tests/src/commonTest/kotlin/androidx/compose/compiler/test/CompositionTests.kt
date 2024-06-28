/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.compiler.test

import androidx.compose.runtime.*
import androidx.compose.runtime.mock.InlineLinear
import androidx.compose.runtime.mock.Text
import androidx.compose.runtime.mock.compositionTest
import androidx.compose.runtime.mock.validate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.setMain
import org.junit.BeforeClass
import kotlin.test.Test
import kotlin.test.assertEquals

class CompositionTests {
    companion object {
        @OptIn(ExperimentalCoroutinesApi::class)
        @BeforeClass
        @JvmStatic
        fun setupMainDispatcher() {
            Dispatchers.setMain(StandardTestDispatcher())
        }
    }

    @Test
    fun composableInAnonymousObjectDeclaration() = compositionTest {
        val list = listOf("a", "b")
        compose {
            list.forEach { s ->
                val obj = object {
                    val value by rememberUpdatedState(s)
                }
                Text(obj.value)
            }
        }

        validate {
            list.forEach {
                Text(it)
            }
        }
    }

    @Test
    fun test_crossinline_indirect() = compositionTest {
        val state = CrossInlineState()

        compose {
            state.place()
        }

        state.show {
            val s = remember { "string" }
            Text(s)
        }
        advance()
        validate {
            Text("string")
        }

        state.show {
            val i = remember { 1 }
            Text("$i")
        }
        advance()
        validate {
            Text("1")
        }
    }

    @Test
    fun composeValueClassDefaultParameter() = compositionTest {
        compose {
            DefaultValueClass()
        }
    }

    @Test
    fun groupAroundIfComposeCallInIfConditionWithShortCircuit() = compositionTest {
        var state by mutableStateOf(true)
        compose {
            ReceiveValue(if (state && getCondition()) 0 else 1)

            ReceiveValue(
                when {
                    state -> when {
                        state -> getCondition()
                        else -> false
                    }.let { if (it) 0 else 1 }
                    else -> 1
                }
            )
        }

        state = false
        advance()
    }

    @Test
    fun returnFromIfInlineNoinline() = compositionTest {
        var state by mutableStateOf(true)
        compose {
            OuterComposable {
                InlineLinear {
                    if (state) return@OuterComposable
                }
            }
        }

        state = false
        advance()
    }
}

@Composable
fun getCondition() = remember { false }

@NonRestartableComposable
@Composable
fun ReceiveValue(value: Int) {
    val string = remember { "$value" }
    assertEquals(1, string.length)
}

class CrossInlineState(content: @Composable () -> Unit = { }) {
    @PublishedApi
    internal var content by mutableStateOf(content)

    inline fun show(crossinline content: @Composable () -> Unit) {
        this.content = { content() }
    }

    @Composable
    fun place() { content() }
}

@JvmInline
value class Data(val string: String)

@Composable
fun DefaultValueClass(
    data: Data = Data("Hello")
) {
    println(data)
}

@Composable
fun OuterComposable(content: @Composable () -> Unit) = content()