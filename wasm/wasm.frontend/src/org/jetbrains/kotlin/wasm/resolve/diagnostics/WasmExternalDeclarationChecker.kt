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

package org.jetbrains.kotlin.wasm.resolve.diagnostics

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.isEnumClass
import org.jetbrains.kotlin.js.resolve.diagnostics.ErrorsJs
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyExternal

object WasmExternalDeclarationChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (descriptor !is MemberDescriptor || !descriptor.isEffectivelyExternal())
            return

        fun reportWrongExternalDeclaration(kind: String) {
            context.trace.report(ErrorsJs.WRONG_EXTERNAL_DECLARATION.on(declaration, kind))
        }

        when (descriptor) {
            is ClassDescriptor -> {
                if (descriptor.kind.isEnumClass) {
                    reportWrongExternalDeclaration("enum class")
                }
            }
            is PropertyDescriptor -> {
                if (descriptor.isLateInit) {
                    reportWrongExternalDeclaration("lateinit property")
                }
            }
            is FunctionDescriptor -> {
                if (descriptor.isTailrec) {
                    reportWrongExternalDeclaration("tailrec function")
                }
                if (descriptor.isSuspend) {
                    reportWrongExternalDeclaration("suspend function")
                }
            }
        }
    }
}
