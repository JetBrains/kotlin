/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.js.translate.intrinsic.functions

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.intrinsic.functions.basic.FunctionIntrinsic
import org.jetbrains.kotlin.js.translate.intrinsic.functions.factories.*

class FunctionIntrinsics {

    private val intrinsicCache = mutableMapOf<FunctionDescriptor, FunctionIntrinsic?>()

    private val factories = listOf(
        LongOperationFIF,
        PrimitiveUnaryOperationFIF.INSTANCE,
        StringPlusCharFIF,
        PrimitiveBinaryOperationFIF.INSTANCE,
        ArrayFIF,
        TopLevelFIF.INSTANCE,
        NumberAndCharConversionFIF,
        ThrowableConstructorIntrinsicFactory,
        ExceptionPropertyIntrinsicFactory,
        AsDynamicFIF,
        CoroutineContextFIF,
        SuspendCoroutineUninterceptedOrReturnFIF,
        InterceptedFIF,
        TypeOfFIF
    )

    fun getIntrinsic(descriptor: FunctionDescriptor, context: TranslationContext): FunctionIntrinsic? {
        if (descriptor in intrinsicCache) return intrinsicCache[descriptor]

        return factories.firstNotNullOfOrNull { it.getIntrinsic(descriptor, context) }.also {
            intrinsicCache[descriptor] = it
        }
    }
}
