/*
 * Copyright 2026 The Android Open Source Project
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
import androidx.compose.runtime.MutableState

// This function and [Unstable] need to be in different files so that the stability of [Unstable] is
// `Stability.Runtime` from the perspective of this function.
@Composable
internal fun SubtypeStabilityNeglectedRegressionTestEntrypoint(state: LoadingState<Unstable>, result: MutableState<Boolean>) {
    LoadingContent(state, result)
}
