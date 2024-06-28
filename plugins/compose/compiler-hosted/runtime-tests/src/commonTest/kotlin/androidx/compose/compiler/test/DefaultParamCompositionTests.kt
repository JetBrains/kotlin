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
import androidx.compose.runtime.mock.Text
import androidx.compose.runtime.mock.compositionTest
import androidx.compose.runtime.mock.validate
import kotlin.test.Test

class DefaultParamCompositionTests {
    @Test
    fun defaultParamInterfaceImpl() = compositionTest {
        val instance = DefaultParamInterfaceImpl()
        compose {
            instance.Content()
            instance.ComposedContent()
            instance.Content { Text("provided") }
            instance.ComposedContent { Text("provided") }
        }

        validate {
            Text("default")
            Text("default")
            Text("provided")
            Text("provided")
        }
    }

    @Test
    fun defaultParamClsImpl() = compositionTest {
        val instance = DefaultParamAbstractImpl()
        compose {
            instance.Content()
            instance.ComposedContent()
            instance.Content { Text("provided") }
            instance.ComposedContent { Text("provided") }
        }

        validate {
            Text("default")
            Text("default")
            Text("provided")
            Text("provided")
        }
    }
}

private interface DefaultParamInterface {
    @Composable fun Content(
        content: @Composable () -> Unit = @Composable { ComposedContent() }
    )
    @Composable fun ComposedContent(
        content: @Composable () -> Unit = @Composable { Text("default") }
    ) {
        content()
    }
}

private class DefaultParamInterfaceImpl : DefaultParamInterface {
    @Composable override fun Content(content: @Composable () -> Unit) {
        content()
    }
    @Composable override fun ComposedContent(content: @Composable () -> Unit) {
        super.ComposedContent(content)
    }
}

private abstract class DefaultParamAbstract {
    @Composable abstract fun Content(
        content: @Composable () -> Unit = @Composable { ComposedContent() }
    )
    @Composable open fun ComposedContent(
        content: @Composable () -> Unit = @Composable { Text("default") }
    ) {
        content()
    }
}

private class DefaultParamAbstractImpl : DefaultParamAbstract() {
    @Composable override fun Content(content: @Composable () -> Unit) {
        content()
    }
    @Composable override fun ComposedContent(content: @Composable () -> Unit) {
        super.ComposedContent(content)
    }
}
