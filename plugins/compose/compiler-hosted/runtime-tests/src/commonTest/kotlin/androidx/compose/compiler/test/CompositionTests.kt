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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mock.Text
import androidx.compose.runtime.mock.compositionTest
import androidx.compose.runtime.mock.validate
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlin.test.Test

class CompositionTests {
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
