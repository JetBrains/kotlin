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

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.js.resolve.MODULE_KIND
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.scopes.receivers.ClassValueReceiver
import org.jetbrains.kotlin.serialization.js.ModuleKind

object JsModuleCallChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val bindingContext = context.trace.bindingContext
        val containingDescriptor = context.scope.ownerDescriptor
        val module = DescriptorUtils.getContainingModule(containingDescriptor)
        val moduleKind = bindingContext[MODULE_KIND, module] ?: return

        val callee = findRoot(extractModuleCallee(resolvedCall) ?: return)
        if (!AnnotationsUtils.isNativeObject(callee)) return

        val callToModule = AnnotationsUtils.getModuleName(callee) != null ||
                           AnnotationsUtils.getFileModuleName(bindingContext, callee) != null
        val callToNonModule = AnnotationsUtils.isNonModule(callee) || AnnotationsUtils.isFromNonModuleFile(bindingContext, callee)

        if (moduleKind == ModuleKind.UMD) {
            if (!callToNonModule && callToModule || callToNonModule && !callToModule) {
                context.trace.report(ErrorsJs.CALL_FROM_UMD_MUST_BE_JS_MODULE_AND_JS_NON_MODULE.on(reportOn))
            }
        }
        else {
            if (moduleKind == ModuleKind.PLAIN) {
                if (!callToNonModule && callToModule) {
                    context.trace.report(ErrorsJs.CALL_TO_JS_MODULE_WITHOUT_MODULE_SYSTEM.on(reportOn))
                }
            }
            else {
                if (!callToModule && callToNonModule) {
                    context.trace.report(ErrorsJs.CALL_TO_JS_NON_MODULE_WITH_MODULE_SYSTEM.on(reportOn))
                }
            }
        }
    }

    private fun extractModuleCallee(call: ResolvedCall<*>): DeclarationDescriptor? {
        val callee = call.resultingDescriptor
        if (DescriptorUtils.isTopLevelDeclaration(callee)) return callee

        val receiver = call.dispatchReceiver ?: return callee
        if (receiver is ClassValueReceiver) return receiver.classQualifier.descriptor

        return null
    }

    private fun findRoot(callee: DeclarationDescriptor) =
            generateSequence(callee) { it.containingDeclaration }
            .takeWhile { it !is PackageFragmentDescriptor }
            .last()
}
