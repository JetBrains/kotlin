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

@file:Suppress("TestFunctionName")

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
    fun groupsInAComplexWhen() = compositionTest {
        val one = listOf(true, true, true, true)
        val two = listOf(false, true, true, true)
        val three = listOf(false, false, true, true)
        val four = listOf(false, false, false, true)
        val five = listOf(false, false, false, false)
        // A permutation of state transitions. This covers all state transitions from A -> B without duplicates
        val states = listOf(
            // 1
            // one, // initial
            // 1 -> 2
            two,
            // 2 -> 1
            one,
            // 1 -> 3
            three,
            // 3 -> 1
            one,
            // 1 -> 4
            four,
            // 4 -> 1
            one,
            // 1 -> 5
            five,
            // 5 -> 2
            two,
            // 2 -> 3
            three,
            // 3 -> 2
            two,
            // 2 -> 4
            four,
            // 4 -> 5
            five,
            // 5 -> 3
            three,
            // 3 -> 5
            five,
            // 5 -> 4
            four,
            // 4 -> 3
            three,
            // 3 -> 4
            four,
            // 4 -> 2
            two,
            // 2 -> 5
            five,
            // 5 -> 1
            one
        )

        var stateA by mutableStateOf(one[0])
        var stateB by mutableStateOf(one[1])
        var stateC by mutableStateOf(one[2])
        var stateD by mutableStateOf(one[3])
        val a = object {
            val value get() = stateA
        }
        val b = object {
            val value get() = stateB
        }
        val c = object {
            val value get() = stateC
        }
        val d = object {
            val value get() = stateD
        }

        compose {
            ExpectValue(
                when {
                    remember { a }.value -> 1
                    remember { b }.value -> 2
                    remember { c }.value -> 3
                    remember { d }.value -> 4
                    else -> 5
                },
                when {
                    stateA -> 1
                    stateB -> 2
                    stateC -> 3
                    stateD -> 4
                    else -> 5
                }
            )
        }

        for (state in states) {
            stateA = state[0]
            stateB = state[1]
            stateC = state[2]
            stateD = state[3]
            advance()
        }
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

@NonRestartableComposable
@Composable
fun ExpectValue(value: Int, expected: Int) {
    assertEquals(expected, value)
}

class CrossInlineState(content: @Composable () -> Unit = { }) {
    @PublishedApi
    internal var content by mutableStateOf(content)

    inline fun show(crossinline content: @Composable () -> Unit) {
        this.content = { content() }
    }

    @Composable
    fun place() {
        content()
    }
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

