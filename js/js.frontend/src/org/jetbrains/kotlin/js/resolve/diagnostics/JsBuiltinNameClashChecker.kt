/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.js.naming.NameSuggestion
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker

class JsBuiltinNameClashChecker(private val nameSuggestion: NameSuggestion) : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (AnnotationsUtils.isNativeObject(descriptor)) return
        if (descriptor.containingDeclaration !is ClassDescriptor) return

        val suggestedName = nameSuggestion.suggest(descriptor)!!
        if (!suggestedName.stable) return
        val simpleName = suggestedName.names.single()

        if (descriptor is ClassDescriptor) {
            if (simpleName in PROHIBITED_STATIC_NAMES) {
                context.trace.report(ErrorsJs.JS_BUILTIN_NAME_CLASH.on(declaration, "Function.$simpleName"))
            }
        }
        else if (descriptor is CallableMemberDescriptor) {
            if (simpleName in PROHIBITED_MEMBER_NAMES) {
                context.trace.report(ErrorsJs.JS_BUILTIN_NAME_CLASH.on(declaration, "Object.prototype.$simpleName"))
            }
        }
    }

    companion object {
        @JvmField
        val PROHIBITED_STATIC_NAMES = setOf("prototype", "length", "\$metadata\$")

        @JvmField
        val PROHIBITED_MEMBER_NAMES = setOf("constructor")
    }
}
