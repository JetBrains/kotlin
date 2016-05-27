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
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.js.naming.FQNGenerator
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DeclarationChecker
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.source.getPsi

class JsNameChecker : DeclarationChecker {
    private val fqnGenerator = FQNGenerator()
    private val scopes = mutableMapOf<DeclarationDescriptor, MutableMap<String, DeclarationDescriptor>>()
    private val clashedDescriptors = mutableSetOf<DeclarationDescriptor>()

    override fun check(
            declaration: KtDeclaration,
            descriptor: DeclarationDescriptor,
            diagnosticHolder: DiagnosticSink,
            bindingContext: BindingContext
    ) {
        checkDescriptor(descriptor, declaration, diagnosticHolder)
    }

    private fun checkDescriptor(descriptor: DeclarationDescriptor, declaration: KtDeclaration, diagnosticHolder: DiagnosticSink) {
        val fqn = fqnGenerator.generate(descriptor)
        if (fqn.shared && fqn.scope is ClassOrPackageFragmentDescriptor && isOpaque(fqn.descriptor)) {
            val scope = getScope(fqn.scope)
            val name = fqn.names.last()
            val existing = scope[name]
            if (existing != null && existing != fqn.descriptor) {
                diagnosticHolder.report(ErrorsJs.JS_NAME_CLASH.on(declaration, name, existing))
                val existingDeclaration = findPsi(existing) ?: declaration
                if (clashedDescriptors.add(existing) && existingDeclaration is KtDeclaration && existingDeclaration != declaration) {
                    diagnosticHolder.report(ErrorsJs.JS_NAME_CLASH.on(existingDeclaration, name, descriptor))
                }
            }
        }
    }

    private fun findPsi(descriptor: DeclarationDescriptor): PsiElement? {
        val psi = (descriptor as? DeclarationDescriptorWithSource)?.source?.getPsi()
        return if (psi == null && descriptor is CallableMemberDescriptor &&
                   descriptor.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE
        ) {
            descriptor.overriddenDescriptors.mapNotNull { findPsi(it) }.firstOrNull()
        }
        else {
            psi
        }
    }

    private fun getScope(descriptor: DeclarationDescriptor) = scopes.getOrPut(descriptor) {
        val scope = mutableMapOf<String, DeclarationDescriptor>()
        when (descriptor) {
            is PackageFragmentDescriptor -> {
                collect(descriptor.getMemberScope(), scope)
                val module = DescriptorUtils.getContainingModule(descriptor)
                module.getSubPackagesOf(descriptor.fqName) { true }
                        .flatMap { module.getPackage(it).fragments }
                        .forEach { collect(it, scope)  }
            }
            is ClassDescriptor -> collect(descriptor.defaultType.memberScope, scope)
        }
        scope
    }

    private fun collect(scope: MemberScope, target: MutableMap<String, DeclarationDescriptor>) {
        for (descriptor in scope.getContributedDescriptors()) {
            val fqn = fqnGenerator.generate(descriptor)
            if (fqn.shared && isOpaque(fqn.descriptor)) {
                target[fqn.names.last()] = fqn.descriptor
            }
        }
    }

    private fun collect(descriptor: DeclarationDescriptor, target: MutableMap<String, DeclarationDescriptor>) {
        val fqn = fqnGenerator.generate(descriptor)
        if (fqn.shared && isOpaque(fqn.descriptor)) {
            target[fqn.names.last()] = fqn.descriptor
        }
    }

    private fun isOpaque(descriptor: DeclarationDescriptor) =
            !AnnotationsUtils.isNativeObject(descriptor) && !AnnotationsUtils.isLibraryObject(descriptor)
}