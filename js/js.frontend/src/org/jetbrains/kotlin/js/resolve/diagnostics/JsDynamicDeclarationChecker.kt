/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.js.resolve.diagnostics

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptorWithAccessors
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtDelegatedSuperTypeEntry
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.types.isDynamic

object JsDynamicDeclarationChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        val trace = context.trace
        if (declaration is KtProperty && descriptor is VariableDescriptorWithAccessors) {
            declaration.delegateExpression?.let { delegateExpression ->
                val provideDelegateCall = trace[BindingContext.PROVIDE_DELEGATE_RESOLVED_CALL, descriptor]
                if (provideDelegateCall != null && provideDelegateCall.resultingDescriptor.returnType?.isDynamic() == true ||
                    trace.getType(delegateExpression)?.isDynamic() == true
                ) {
                    trace.report(ErrorsJs.PROPERTY_DELEGATION_BY_DYNAMIC.on(delegateExpression))
                }
            }
        }
        else if (declaration is KtClassOrObject) {
            for (delegateDecl in declaration.superTypeListEntries.filterIsInstance<KtDelegatedSuperTypeEntry>()) {
                val delegateExpr = delegateDecl.delegateExpression ?: continue
                if (trace.getType(delegateExpr)?.isDynamic() == true) {
                    trace.report(ErrorsJs.DELEGATION_BY_DYNAMIC.on(delegateExpr))
                }
            }
        }
    }
}
