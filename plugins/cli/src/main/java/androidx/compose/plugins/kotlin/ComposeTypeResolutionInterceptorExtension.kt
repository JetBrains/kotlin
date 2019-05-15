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

package androidx.compose.plugins.kotlin

import androidx.compose.plugins.kotlin.analysis.ComposeWritableSlices.INFERRED_COMPOSABLE_DESCRIPTOR
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.extensions.TypeResolutionInterceptorExtension
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.expressions.ExpressionTypingContext

/**
 * If a lambda is marked as `@Composable`, then the inferred type should become `@Composable`
 */
class ComposeTypeResolutionInterceptorExtension : TypeResolutionInterceptorExtension {

    override fun interceptFunctionLiteralDescriptor(
        expression: KtLambdaExpression,
        context: ExpressionTypingContext,
        descriptor: AnonymousFunctionDescriptor
    ): AnonymousFunctionDescriptor {
        if (context.expectedType.hasComposableAnnotation()) {
            // If the expected type has an @Composable annotation then the literal function
            // expression should infer a an @Composable annotation
            context.trace.record(INFERRED_COMPOSABLE_DESCRIPTOR, descriptor, true)
        }
        return descriptor
    }

    override fun interceptType(
        element: KtElement,
        context: ExpressionTypingContext,
        resultType: KotlinType
    ): KotlinType {
        if (resultType === TypeUtils.NO_EXPECTED_TYPE) return resultType
        if (element !is KtLambdaExpression) return resultType
        val module = context.scope.ownerDescriptor.module
        val checker =
            StorageComponentContainerContributor.getInstances(element.project).single {
                it is ComposableAnnotationChecker
            } as ComposableAnnotationChecker
        if ((context.expectedType.hasComposableAnnotation() || checker.analyze(
                context.trace,
                element,
                resultType
            ) != ComposableAnnotationChecker.Composability.NOT_COMPOSABLE)
        ) {
            return resultType.makeComposable(module)
        }
        return resultType
    }
}
