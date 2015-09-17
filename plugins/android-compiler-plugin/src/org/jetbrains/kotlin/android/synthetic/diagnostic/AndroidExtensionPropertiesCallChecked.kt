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

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.android.synthetic.AndroidConst
import org.jetbrains.kotlin.android.synthetic.CliAndroidDeclarationsProvider
import org.jetbrains.kotlin.android.synthetic.res.SyntheticFileGenerator
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.android.synthetic.diagnostic.ErrorsAndroid.*
import org.jetbrains.kotlin.extensions.ExternalDeclarationsProvider

public class AndroidExtensionPropertiesCallChecker : CallChecker {
    override fun <F : CallableDescriptor> check(resolvedCall: ResolvedCall<F>, context: BasicCallResolutionContext) {
        val expression = context.call.calleeExpression ?: return

        val propertyDescriptor = resolvedCall.resultingDescriptor as? PropertyDescriptor ?: return
        val syntheticPackage = propertyDescriptor.containingDeclaration as? PackageFragmentDescriptor ?: return
        if (!syntheticPackage.fqName.asString().startsWith("${AndroidConst.SYNTHETIC_PACKAGE}.")) return

        val invalidWidgetTypeAnnotation = propertyDescriptor.annotations.findAnnotation(
                SyntheticFileGenerator.INVALID_WIDGET_TYPE_ANNOTATION_FQNAME) ?: return

        val type = invalidWidgetTypeAnnotation.allValueArguments.filterKeys {
            it.name.asString() == SyntheticFileGenerator.INVALID_WIDGET_TYPE_ANNOTATION_TYPE_PARAMETER
        }.values().firstOrNull() as? StringValue ?: return

        val erroneousType = type.value
        val warning = if (erroneousType.contains('.')) SYNTHETIC_UNRESOLVED_WIDGET_TYPE else SYNTHETIC_INVALID_WIDGET_TYPE
        context.trace.report(warning.on(expression, erroneousType))
    }
}