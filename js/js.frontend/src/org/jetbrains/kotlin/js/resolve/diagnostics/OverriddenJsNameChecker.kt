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

import org.jetbrains.kotlin.config.LanguageFeatureSettings
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.js.naming.FQNGenerator
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DeclarationChecker

class OverriddenJsNameChecker : DeclarationChecker {
    private val fqnGenerator = FQNGenerator()

    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, diagnosticHolder: DiagnosticSink,
                       bindingContext: BindingContext, languageFeatureSettings: LanguageFeatureSettings) {
        doCheck(descriptor) { first, second ->
            val psi = descriptor.findPsi() ?: declaration
            diagnosticHolder.report(ErrorsJs.JS_NAME_OVERRIDE_CLASH.on(psi, first, second))
        }

        if (descriptor is ClassDescriptor) {
            val fakeOverrides = descriptor.defaultType.memberScope.getContributedDescriptors().asSequence()
                    .mapNotNull { it as? CallableMemberDescriptor }
                    .filter { it.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE }
            for (override in fakeOverrides) {
                val errorFound = doCheck(override) { first, second ->
                    val psi = descriptor.findPsi() ?: declaration
                    diagnosticHolder.report(ErrorsJs.JS_NAME_OVERRIDE_CLASH.on(psi, first, second))
                }
                if (errorFound) break
            }
        }
    }

    private fun doCheck(descriptor: DeclarationDescriptor, onClash: (CallableMemberDescriptor, CallableMemberDescriptor) -> Unit):
            Boolean {
        if (descriptor !is CallableMemberDescriptor) return false

        var knownName: String? = null
        var knownDescriptor: CallableMemberDescriptor? = null
        for (overridden in descriptor.overriddenDescriptors) {
            val fqn = fqnGenerator.generate(overridden)
            if (!fqn.shared) continue

            val name = fqnGenerator.generate(overridden).names.last()
            if (knownName == null) {
                knownName = name
                knownDescriptor = overridden
            }
            else if (knownName != name &&
                     (AnnotationsUtils.getJsName(knownDescriptor!!) != null || AnnotationsUtils.getJsName(overridden) != null)) {
                onClash(knownDescriptor, overridden)
                return true
            }
        }

        return false
    }
}
