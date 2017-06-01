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

package org.jetbrains.kotlin.android.synthetic.diagnostic

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.android.synthetic.descriptors.AndroidSyntheticPackageFragmentDescriptor
import org.jetbrains.kotlin.android.synthetic.diagnostic.ErrorsAndroid.*
import org.jetbrains.kotlin.android.synthetic.res.AndroidSyntheticProperty
import org.jetbrains.kotlin.android.synthetic.res.isErrorType
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.isFlexible

class AndroidExtensionPropertiesCallChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        reportOn as? KtExpression ?: return

        val propertyDescriptor = resolvedCall.resultingDescriptor as? PropertyDescriptor ?: return
        val containingPackage = propertyDescriptor.containingDeclaration as? AndroidSyntheticPackageFragmentDescriptor ?: return
        val androidSyntheticProperty = propertyDescriptor as? AndroidSyntheticProperty ?: return

        with(context.trace) {
            checkUnresolvedWidgetType(reportOn, androidSyntheticProperty)
            checkDeprecated(reportOn, containingPackage)
            checkPartiallyDefinedResource(resolvedCall, androidSyntheticProperty, context)
        }
    }

    private fun DiagnosticSink.checkDeprecated(expression: KtExpression, packageDescriptor: AndroidSyntheticPackageFragmentDescriptor) {
        if (packageDescriptor.packageData.isDeprecated) {
            report(SYNTHETIC_DEPRECATED_PACKAGE.on(expression))
        }
    }

    private fun DiagnosticSink.checkUnresolvedWidgetType(expression: KtExpression, property: AndroidSyntheticProperty) {
        if (!property.isErrorType) return
        val type = property.errorType ?: return

        val warning = if (type.contains('.')) SYNTHETIC_UNRESOLVED_WIDGET_TYPE else SYNTHETIC_INVALID_WIDGET_TYPE
        report(warning.on(expression, type))
    }

    private fun DiagnosticSink.checkPartiallyDefinedResource(
            resolvedCall: ResolvedCall<*>,
            property: AndroidSyntheticProperty,
            context: CallCheckerContext
    ) {
        if (!property.resource.partiallyDefined) return
        val calleeExpression = resolvedCall.call.calleeExpression ?: return

        val expectedType = context.resolutionContext.expectedType
        if (!TypeUtils.noExpectedType(expectedType) && !expectedType.isMarkedNullable && !expectedType.isFlexible()) {
            report(UNSAFE_CALL_ON_PARTIALLY_DEFINED_RESOURCE.on(calleeExpression))
            return
        }

        val outermostQualifiedExpression = findLeftOutermostQualifiedExpression(calleeExpression) ?: return
        val usage = outermostQualifiedExpression.parent

        if (usage is KtDotQualifiedExpression && usage.receiverExpression == outermostQualifiedExpression) {
            report(UNSAFE_CALL_ON_PARTIALLY_DEFINED_RESOURCE.on(calleeExpression))
        }
    }

    private fun findLeftOutermostQualifiedExpression(calleeExpression: KtExpression?): KtElement? {
        val parent = calleeExpression?.parent ?: return null

        if (parent is KtQualifiedExpression && parent.selectorExpression == calleeExpression) {
            return findLeftOutermostQualifiedExpression(parent)
        }

        return calleeExpression
    }
}
