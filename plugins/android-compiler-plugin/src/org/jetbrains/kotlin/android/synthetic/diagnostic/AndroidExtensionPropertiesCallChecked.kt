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

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.android.synthetic.diagnostic.ErrorsAndroid.*
import org.jetbrains.kotlin.android.synthetic.descriptors.AndroidSyntheticPackageFragmentDescriptor
import org.jetbrains.kotlin.android.synthetic.res.AndroidSyntheticProperty
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.psi.KtExpression

public class AndroidExtensionPropertiesCallChecker : CallChecker {
    override fun <F : CallableDescriptor> check(resolvedCall: ResolvedCall<F>, context: BasicCallResolutionContext) {
        val expression = context.call.calleeExpression ?: return

        val propertyDescriptor = resolvedCall.resultingDescriptor as? PropertyDescriptor ?: return
        val containingPackage = propertyDescriptor.containingDeclaration as? AndroidSyntheticPackageFragmentDescriptor ?: return
        val androidSyntheticProperty = propertyDescriptor as? AndroidSyntheticProperty ?: return

        with (context.trace) {
            checkUnresolvedWidgetType(expression, androidSyntheticProperty)
            checkDeprecated(expression, containingPackage)
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

}