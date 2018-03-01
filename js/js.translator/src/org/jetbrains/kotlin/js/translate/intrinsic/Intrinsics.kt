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

package org.jetbrains.kotlin.js.translate.intrinsic

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.intrinsic.functions.FunctionIntrinsics
import org.jetbrains.kotlin.js.translate.intrinsic.functions.basic.FunctionIntrinsic
import org.jetbrains.kotlin.js.translate.intrinsic.objects.ObjectIntrinsic
import org.jetbrains.kotlin.js.translate.intrinsic.objects.ObjectIntrinsics
import org.jetbrains.kotlin.js.translate.intrinsic.operation.BinaryOperationIntrinsic
import org.jetbrains.kotlin.js.translate.intrinsic.operation.BinaryOperationIntrinsics
import org.jetbrains.kotlin.psi.KtBinaryExpression

/**
 * Provides mechanism to substitute method calls /w native constructs directly.
 */
class Intrinsics {
    private val functionIntrinsics = FunctionIntrinsics()
    private val binaryOperationIntrinsics = BinaryOperationIntrinsics()
    private val objectIntrinsics = ObjectIntrinsics()

    fun getBinaryOperationIntrinsic(expression: KtBinaryExpression, context: TranslationContext): BinaryOperationIntrinsic? {
        return binaryOperationIntrinsics.getIntrinsic(expression, context)
    }

    fun getFunctionIntrinsic(descriptor: FunctionDescriptor): FunctionIntrinsic? {
        return functionIntrinsics.getIntrinsic(descriptor)
    }

    fun getObjectIntrinsic(classDescriptor: ClassDescriptor): ObjectIntrinsic {
        return objectIntrinsics.getIntrinsic(classDescriptor)
    }
}