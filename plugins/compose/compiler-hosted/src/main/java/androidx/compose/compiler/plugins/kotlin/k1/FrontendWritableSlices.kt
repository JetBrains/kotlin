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

package androidx.compose.compiler.plugins.kotlin.k1

import androidx.compose.compiler.plugins.kotlin.inference.LazyScheme
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.util.slicedMap.BasicWritableSlice
import org.jetbrains.kotlin.util.slicedMap.RewritePolicy
import org.jetbrains.kotlin.util.slicedMap.WritableSlice

object FrontendWritableSlices {
    val INFERRED_COMPOSABLE_DESCRIPTOR: WritableSlice<FunctionDescriptor, Boolean> =
        BasicWritableSlice(RewritePolicy.DO_NOTHING)
    val LAMBDA_CAPABLE_OF_COMPOSER_CAPTURE: WritableSlice<FunctionDescriptor, Boolean> =
        BasicWritableSlice(RewritePolicy.DO_NOTHING)
    val INFERRED_COMPOSABLE_LITERAL: WritableSlice<KtLambdaExpression, Boolean> =
        BasicWritableSlice(RewritePolicy.DO_NOTHING)
    val COMPOSE_LAZY_SCHEME: WritableSlice<Any, LazyScheme> =
        BasicWritableSlice(RewritePolicy.DO_NOTHING)
}
