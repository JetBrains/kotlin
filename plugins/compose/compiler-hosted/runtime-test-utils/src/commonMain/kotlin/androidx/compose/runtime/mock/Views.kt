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

package androidx.compose.runtime.mock

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.ComposeNodeLifecycleCallback
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReusableComposeNode
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState

@Composable
fun <T : Any> Repeated(of: Iterable<T>, block: @Composable (value: T) -> Unit) {
    for (value in of) {
        key(value) { block(value) }
    }
}

@Composable
fun Linear(content: @Composable () -> Unit) {
    ReusableComposeNode<View, ViewApplier>(
        factory = { View().also { it.name = "linear" } },
        update = {},
        content = content,
    )
}

@Composable
inline fun InlineLinear(content: @Composable () -> Unit) {
    ReusableComposeNode<View, ViewApplier>(
        factory = { View().also { it.name = "linear" } },
        update = {},
    ) {
        content()
    }
}

@Composable
fun Linear(
    onReuse: () -> Unit = {},
    onDeactivate: () -> Unit = {},
    onRelease: () -> Unit = {},
    onSet: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val currentOnReuse by rememberUpdatedState(onReuse)
    val currentOnDeactivate by rememberUpdatedState(onDeactivate)
    val currentOnRelease by rememberUpdatedState(onRelease)
    ReusableComposeNode<View, ViewApplier>(
        factory = {
            object : View(), ComposeNodeLifecycleCallback {
                init {
                    name = "linear"
                }

                override fun onRelease() {
                    currentOnRelease()
                }

                override fun onReuse() {
                    currentOnReuse()
                }

                override fun onDeactivate() {
                    currentOnDeactivate()
                }
            }
        },
        update = { set(onSet) { onSet() } },
        content = content,
    )
}

@Composable
fun NonReusableLinear(content: @Composable () -> Unit) {
    ComposeNode<View, ViewApplier>(
        factory = { View().also { it.name = "linear" } },
        update = {},
        content = content,
    )
}

@Composable
@NonRestartableComposable
fun Text(value: String) {
    ReusableComposeNode<View, ViewApplier>(
        factory = { View().also { it.name = "text" } },
        update = { set(value) { text = it } },
    )
}

@Composable
fun NonReusableText(value: String) {
    ComposeNode<View, ViewApplier>(
        factory = { View().also { it.name = "text" } },
        update = { set(value) { text = it } },
    )
}

@Composable
fun Edit(value: String) {
    ReusableComposeNode<View, ViewApplier>(
        factory = { View().also { it.name = "edit" } },
        update = { set(value) { this.value = it } },
    )
}

@Composable
fun SelectBox(selected: Boolean, content: @Composable () -> Unit) {
    if (selected) {
        ReusableComposeNode<View, ViewApplier>(
            factory = { View().also { it.name = "box" } },
            update = {},
            content = content,
        )
    } else {
        content()
    }
}
