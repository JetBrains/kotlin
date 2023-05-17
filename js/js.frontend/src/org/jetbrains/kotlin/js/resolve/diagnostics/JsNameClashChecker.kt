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

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.js.naming.JsNameSuggestion
import org.jetbrains.kotlin.js.naming.NameSuggestion
import org.jetbrains.kotlin.js.naming.SuggestedName
import org.jetbrains.kotlin.js.naming.WasmNameSuggestion
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorEquivalenceForOverrides
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtensionProperty
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner

class JsNameClashChecker(
    nameSuggestion: JsNameSuggestion,
    languageVersionSettings: LanguageVersionSettings,
    kotlinTypeRefiner: KotlinTypeRefiner,
) : AbstractNameClashChecker(
    nameSuggestion,
    languageVersionSettings,
    kotlinTypeRefiner,
)

class WasmNameClashChecker(
    nameSuggestion: WasmNameSuggestion,
    languageVersionSettings: LanguageVersionSettings,
    kotlinTypeRefiner: KotlinTypeRefiner,
) : AbstractNameClashChecker(
    nameSuggestion,
    languageVersionSettings,
    kotlinTypeRefiner,
)

abstract class AbstractNameClashChecker(
    private val nameSuggestion: NameSuggestion,
    private val languageVersionSettings: LanguageVersionSettings,
    private val kotlinTypeRefiner: KotlinTypeRefiner,
) : DeclarationChecker {
    companion object {
        private val COMMON_DIAGNOSTICS = setOf(
                Errors.REDECLARATION,
                Errors.CONFLICTING_OVERLOADS,
                Errors.PACKAGE_OR_CLASSIFIER_REDECLARATION)
    }

    private val scopes = mutableMapOf<DeclarationDescriptor, MutableMap<String, DeclarationDescriptor>>()
    private val clashedFakeOverrides = mutableMapOf<DeclarationDescriptor, Pair<DeclarationDescriptor, DeclarationDescriptor>>()
    private val clashedDescriptors = mutableSetOf<Pair<DeclarationDescriptor, String>>()

    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        // We don't generate JS properties for extension properties, we generate methods instead, so in this case
        // check name clash only for accessors, not properties
        if (!descriptor.isExtensionProperty) {
            checkDescriptor(descriptor, declaration, context.trace, context.trace.bindingContext)
        }
    }

    private fun checkDescriptor(
            descriptor: DeclarationDescriptor, declaration: KtDeclaration,
            diagnosticHolder: DiagnosticSink, bindingContext: BindingContext
    ) {
        if (descriptor is ConstructorDescriptor && descriptor.isPrimary) return

        for (suggested in nameSuggestion.suggestAllPossibleNames(descriptor, bindingContext)) {
            if (suggested.stable && suggested.scope is ClassOrPackageFragmentDescriptor && presentsInGeneratedCode(suggested.descriptor)) {
                val scope = getScope(suggested.scope, bindingContext)
                val name = suggested.names.last()
                val existing = scope[name]
                if (existing != null &&
                    existing != descriptor &&
                    existing.isActual == descriptor.isActual &&
                    existing.isExpect == descriptor.isExpect &&
                    !bindingContext.isCommonDiagnosticReported(declaration)
                ) {
                    diagnosticHolder.report(ErrorsJs.JS_NAME_CLASH.on(declaration, name, existing))
                    val existingDeclaration = existing.findPsi()
                    if (clashedDescriptors.add(existing to name) && existingDeclaration is KtDeclaration &&
                        existingDeclaration != declaration) {
                        diagnosticHolder.report(ErrorsJs.JS_NAME_CLASH.on(existingDeclaration, name, descriptor))
                    }
                }
            }
        }

        if (descriptor is ClassDescriptor) {
            val fakeOverrides = descriptor.unsubstitutedMemberScope.getContributedDescriptors().asSequence()
                    .mapNotNull { it as? CallableMemberDescriptor }
                    .filter { it.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE }
            for (override in fakeOverrides) {
                val overrideFqn = nameSuggestion.suggest(override, bindingContext)!!
                val scope = getScope(overrideFqn.scope, bindingContext)
                val name = overrideFqn.names.last()
                val existing = scope[name] as? CallableMemberDescriptor
                val overrideDescriptor = overrideFqn.descriptor as? CallableMemberDescriptor
                if (existing != null &&
                    overrideDescriptor != null &&
                    !areDescriptorsEquivalent(existing, overrideDescriptor) &&
                    !isFakeOverridingNative(existing)
                ) {
                    diagnosticHolder.report(ErrorsJs.JS_FAKE_NAME_CLASH.on(declaration, name, override, existing))
                    break
                }

                val clashedOverrides = clashedFakeOverrides[override]
                if (clashedOverrides != null) {
                    val (firstExample, secondExample) = clashedOverrides
                    diagnosticHolder.report(ErrorsJs.JS_FAKE_NAME_CLASH.on(declaration, name, firstExample, secondExample))
                    break
                }
            }
        }
    }

    private fun areDescriptorsEquivalent(
        existing: CallableMemberDescriptor,
        overrideDescriptor: CallableMemberDescriptor
    ): Boolean {
        return if (kotlinTypeRefiner === KotlinTypeRefiner.Default) {
            // Fast-path
            existing == overrideDescriptor
        } else {
            // If refinement is enabled, we can get duplicate descriptors for one and the same members (as refinement re-creates
            // descriptors), so, in this case, we have to compare descriptors structurally
            DescriptorEquivalenceForOverrides.areCallableDescriptorsEquivalent(
                existing, overrideDescriptor, allowCopiesFromTheSameDeclaration = true, kotlinTypeRefiner = kotlinTypeRefiner
            )
        }
    }

    private fun NameSuggestion.suggestAllPossibleNames(descriptor: DeclarationDescriptor, bindingContext: BindingContext): Collection<SuggestedName> =
            if (descriptor is CallableMemberDescriptor) {
                val primary = suggest(descriptor, bindingContext)
                if (primary != null) {
                    val overriddenNames = descriptor.overriddenDescriptors.flatMap {
                        suggestAllPossibleNames(it, bindingContext).map { overridden ->
                            SuggestedName(overridden.names, overridden.stable, primary.descriptor, primary.scope)
                        }
                    }
                    (overriddenNames + primary).distinctBy { it.names }
                }
                else {
                    emptyList()
                }
            }
            else {
                listOfNotNull(suggest(descriptor, bindingContext))
            }

    private fun BindingContext.isCommonDiagnosticReported(declaration: KtDeclaration): Boolean {
        return diagnostics.forElement(declaration).any { it.factory in COMMON_DIAGNOSTICS }
    }

    private val DeclarationDescriptor.isActual: Boolean
        get() = this is MemberDescriptor && this.isActual || this is PropertyAccessorDescriptor && this.correspondingProperty.isActual

    private val DeclarationDescriptor.isExpect: Boolean
        get() = this is MemberDescriptor && this.isExpect || this is PropertyAccessorDescriptor && this.correspondingProperty.isExpect

    private fun isFakeOverridingNative(descriptor: CallableMemberDescriptor): Boolean {
        return descriptor.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE &&
                descriptor.overriddenDescriptors.all { !presentsInGeneratedCode(it) }
    }

    private fun getScope(descriptor: DeclarationDescriptor, bindingContext: BindingContext) = scopes.getOrPut(descriptor) {
        val scope = mutableMapOf<String, DeclarationDescriptor>()
        when (descriptor) {
            is PackageFragmentDescriptor -> {
                collect(descriptor.getMemberScope(), scope, bindingContext)
                val module = DescriptorUtils.getContainingModule(descriptor)
                module.getSubPackagesOf(descriptor.fqName) { true }
                        .flatMap { module.getPackage(it).fragments }
                        .forEach { collect(it, scope, bindingContext)  }
            }
            is ClassDescriptor -> collect(descriptor.defaultType.memberScope, scope, bindingContext)
        }
        scope
    }

    private fun collect(scope: MemberScope, target: MutableMap<String, DeclarationDescriptor>, bindingContext: BindingContext) {
        for (descriptor in scope.getContributedDescriptors()) {
            collect(descriptor, target, bindingContext)
        }
    }

    private fun collect(descriptor: DeclarationDescriptor, target: MutableMap<String, DeclarationDescriptor>, bindingContext: BindingContext) {
        if (descriptor is PropertyDescriptor) {
            if (descriptor.isExtension || AnnotationsUtils.hasJsNameInAccessors(descriptor)) {
                descriptor.accessors.forEach { collect(it, target, bindingContext) }
                return
            }
        }

        for (fqn in nameSuggestion.suggestAllPossibleNames(descriptor, bindingContext)) {
            if (fqn.stable && presentsInGeneratedCode(fqn.descriptor)) {
                target[fqn.names.last()] = fqn.descriptor
                (fqn.descriptor as? CallableMemberDescriptor)?.let { checkOverrideClashes(it, target, bindingContext) }
            }
        }
    }

    private fun checkOverrideClashes(descriptor: CallableMemberDescriptor, target: MutableMap<String, DeclarationDescriptor>, bindingContext: BindingContext) {
        for (overriddenDescriptor in DescriptorUtils.getAllOverriddenDeclarations(descriptor)) {
            val overriddenFqn = nameSuggestion.suggest(overriddenDescriptor, bindingContext)!!
            if (overriddenFqn.stable) {
                val existing = target[overriddenFqn.names.last()]
                if (existing != null) {
                    if (existing != descriptor && descriptor.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
                        clashedFakeOverrides[descriptor] = Pair(existing, overriddenDescriptor)
                    }
                }
                else {
                    target[overriddenFqn.names.last()] = descriptor
                }
            }
        }
    }

    private fun presentsInGeneratedCode(descriptor: DeclarationDescriptor) =
            !AnnotationsUtils.isNativeObject(descriptor) && !AnnotationsUtils.isLibraryObject(descriptor)
}
