/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.resolve.diagnostics

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.isExtensionFunctionType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.js.PredefinedAnnotation
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.isInlineClassType
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils

object JsExternalChecker : DeclarationChecker {
    val DEFINED_EXTERNALLY_PROPERTY_NAMES = setOf(FqNameUnsafe("kotlin.js.noImpl"), FqNameUnsafe("kotlin.js.definedExternally"))

    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (!AnnotationsUtils.isNativeObject(descriptor)) return

        val trace = context.trace
        if (!DescriptorUtils.isTopLevelDeclaration(descriptor)) {
            if (isDirectlyExternal(declaration, descriptor) && descriptor !is PropertyAccessorDescriptor) {
                trace.report(ErrorsJs.NESTED_EXTERNAL_DECLARATION.on(declaration))
            }
        }

        if (descriptor is ClassDescriptor) {
            val classKind = when {
                descriptor.isData -> "data class"
                descriptor.isInner -> "inner class"
                descriptor.isInline -> "inline class"
                DescriptorUtils.isAnnotationClass(descriptor) -> "annotation class"
                else -> null
            }

            if (classKind != null) {
                trace.report(ErrorsJs.WRONG_EXTERNAL_DECLARATION.on(declaration, classKind))
            }
        }

        if (descriptor is PropertyAccessorDescriptor && isDirectlyExternal(declaration, descriptor)) {
            trace.report(ErrorsJs.WRONG_EXTERNAL_DECLARATION.on(declaration, "property accessor"))
        } else if (isPrivateMemberOfExternalClass(descriptor)) {
            trace.report(ErrorsJs.WRONG_EXTERNAL_DECLARATION.on(declaration, "private member of class"))
        }

        if (descriptor is ClassDescriptor && descriptor.kind != ClassKind.INTERFACE &&
            descriptor.containingDeclaration.let { it is ClassDescriptor && it.kind == ClassKind.INTERFACE }
        ) {
            trace.report(ErrorsJs.NESTED_CLASS_IN_EXTERNAL_INTERFACE.on(declaration))
        }

        if (descriptor !is PropertyAccessorDescriptor && descriptor.isExtension) {
            val target = when (descriptor) {
                is FunctionDescriptor -> "extension function"
                is PropertyDescriptor -> "extension property"
                else -> "extension member"
            }
            trace.report(ErrorsJs.WRONG_EXTERNAL_DECLARATION.on(declaration, target))
        }

        if (descriptor is ClassDescriptor && descriptor.kind != ClassKind.ANNOTATION_CLASS) {
            val superClasses = (listOfNotNull(descriptor.getSuperClassNotAny()) + descriptor.getSuperInterfaces()).toMutableSet()
            if (descriptor.kind == ClassKind.ENUM_CLASS || descriptor.kind == ClassKind.ENUM_ENTRY) {
                superClasses.removeAll { it.fqNameUnsafe == KotlinBuiltIns.FQ_NAMES._enum }
            }
            if (superClasses.any { !AnnotationsUtils.isNativeObject(it) && it.fqNameSafe != KotlinBuiltIns.FQ_NAMES.throwable }) {
                trace.report(ErrorsJs.EXTERNAL_TYPE_EXTENDS_NON_EXTERNAL_TYPE.on(declaration))
            }
        }

        if (descriptor is FunctionDescriptor && descriptor.isInline) {
            trace.report(ErrorsJs.INLINE_EXTERNAL_DECLARATION.on(declaration))
        }

        fun reportOnParametersAndReturnTypesIf(
            diagnosticFactory: DiagnosticFactory0<KtElement>,
            condition: (KotlinType) -> Boolean
        ) {
            if (descriptor is CallableMemberDescriptor && !(descriptor is PropertyAccessorDescriptor && descriptor.isDefault)) {
                fun checkTypeIsNotInlineClass(type: KotlinType, elementToReport: KtElement) {
                    if (condition(type)) {
                        trace.report(diagnosticFactory.on(elementToReport))
                    }
                }

                for (p in descriptor.valueParameters) {
                    val ktParam = p.source.getPsi() as? KtParameter ?: declaration
                    checkTypeIsNotInlineClass(p.varargElementType ?: p.type, ktParam)
                }

                val elementToReport = when (declaration) {
                    is KtCallableDeclaration -> declaration.typeReference
                    is KtPropertyAccessor -> declaration.returnTypeReference
                    else -> declaration
                }

                elementToReport?.let {
                    checkTypeIsNotInlineClass(descriptor.returnType!!, it)
                }
            }
        }

        reportOnParametersAndReturnTypesIf(ErrorsJs.INLINE_CLASS_IN_EXTERNAL_DECLARATION, KotlinType::isInlineClassType)
        reportOnParametersAndReturnTypesIf(ErrorsJs.EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION, KotlinType::isExtensionFunctionType)

        if (descriptor is CallableMemberDescriptor && descriptor.isNonAbstractMemberOfInterface() &&
            !descriptor.isNullableProperty()
        ) {
            trace.report(ErrorsJs.NON_ABSTRACT_MEMBER_OF_EXTERNAL_INTERFACE.on(declaration))
        }

        checkBody(declaration, descriptor, trace, trace.bindingContext)
        checkDelegation(declaration, descriptor, trace)
        checkAnonymousInitializer(declaration, trace)
        checkEnumEntry(declaration, trace)
        checkConstructorPropertyParam(declaration, descriptor, trace)
    }

    private fun checkBody(
        declaration: KtDeclaration, descriptor: DeclarationDescriptor,
        diagnosticHolder: DiagnosticSink, bindingContext: BindingContext
    ) {
        if (declaration is KtProperty && descriptor is PropertyAccessorDescriptor) return

        if (declaration is KtDeclarationWithBody && !declaration.hasValidExternalBody(bindingContext)) {
            diagnosticHolder.report(ErrorsJs.WRONG_BODY_OF_EXTERNAL_DECLARATION.on(declaration.bodyExpression!!))
        } else if (declaration is KtDeclarationWithInitializer &&
            declaration.initializer?.isDefinedExternallyExpression(bindingContext) == false
        ) {
            diagnosticHolder.report(ErrorsJs.WRONG_INITIALIZER_OF_EXTERNAL_DECLARATION.on(declaration.initializer!!))
        }
        if (declaration is KtCallableDeclaration) {
            for (defaultValue in declaration.valueParameters.mapNotNull { it.defaultValue }) {
                if (!defaultValue.isDefinedExternallyExpression(bindingContext)) {
                    diagnosticHolder.report(ErrorsJs.WRONG_DEFAULT_VALUE_FOR_EXTERNAL_FUN_PARAMETER.on(defaultValue))
                }
            }
        }
    }

    private fun checkDelegation(declaration: KtDeclaration, descriptor: DeclarationDescriptor, diagnosticHolder: DiagnosticSink) {
        if (descriptor !is MemberDescriptor || !descriptor.isEffectivelyExternal()) return

        if (declaration is KtClassOrObject) {
            for (superTypeEntry in declaration.superTypeListEntries) {
                when (superTypeEntry) {
                    is KtSuperTypeCallEntry -> {
                        diagnosticHolder.report(ErrorsJs.EXTERNAL_DELEGATED_CONSTRUCTOR_CALL.on(superTypeEntry.valueArgumentList!!))
                    }
                    is KtDelegatedSuperTypeEntry -> {
                        diagnosticHolder.report(ErrorsJs.EXTERNAL_DELEGATION.on(superTypeEntry))
                    }
                }
            }
        } else if (declaration is KtSecondaryConstructor) {
            val delegationCall = declaration.getDelegationCall()
            if (!delegationCall.isImplicit) {
                diagnosticHolder.report(ErrorsJs.EXTERNAL_DELEGATED_CONSTRUCTOR_CALL.on(delegationCall))
            }
        } else if (declaration is KtProperty && descriptor !is PropertyAccessorDescriptor) {
            declaration.delegate?.let { delegate ->
                diagnosticHolder.report(ErrorsJs.EXTERNAL_DELEGATION.on(delegate))
            }
        }
    }

    private fun checkAnonymousInitializer(declaration: KtDeclaration, diagnosticHolder: DiagnosticSink) {
        if (declaration !is KtClassOrObject) return

        for (anonymousInitializer in declaration.getAnonymousInitializers()) {
            diagnosticHolder.report(ErrorsJs.EXTERNAL_ANONYMOUS_INITIALIZER.on(anonymousInitializer))
        }
    }

    private fun checkEnumEntry(declaration: KtDeclaration, diagnosticHolder: DiagnosticSink) {
        if (declaration !is KtEnumEntry) return
        declaration.body?.let {
            diagnosticHolder.report(ErrorsJs.EXTERNAL_ENUM_ENTRY_WITH_BODY.on(it))
        }
    }

    private fun checkConstructorPropertyParam(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        diagnosticHolder: DiagnosticSink
    ) {
        if (descriptor !is PropertyDescriptor || declaration !is KtParameter) return
        val containingClass = descriptor.containingDeclaration as ClassDescriptor
        if (containingClass.isData || DescriptorUtils.isAnnotationClass(containingClass)) return
        diagnosticHolder.report(ErrorsJs.EXTERNAL_CLASS_CONSTRUCTOR_PROPERTY_PARAMETER.on(declaration))
    }

    private fun isDirectlyExternal(declaration: KtDeclaration, descriptor: DeclarationDescriptor): Boolean {
        if (declaration is KtProperty && descriptor is PropertyAccessorDescriptor) return false

        return declaration.hasModifier(KtTokens.EXTERNAL_KEYWORD) ||
                AnnotationsUtils.hasAnnotation(descriptor, PredefinedAnnotation.NATIVE)
    }

    private fun isPrivateMemberOfExternalClass(descriptor: DeclarationDescriptor): Boolean {
        if (descriptor is PropertyAccessorDescriptor && descriptor.visibility == descriptor.correspondingProperty.visibility) return false
        if (descriptor !is MemberDescriptor || descriptor.visibility != Visibilities.PRIVATE) return false

        val containingDeclaration = descriptor.containingDeclaration as? ClassDescriptor ?: return false
        return AnnotationsUtils.isNativeObject(containingDeclaration)
    }

    private fun CallableMemberDescriptor.isNonAbstractMemberOfInterface() =
        modality != Modality.ABSTRACT &&
                DescriptorUtils.isInterface(containingDeclaration) &&
                this !is PropertyAccessorDescriptor

    private fun CallableMemberDescriptor.isNullableProperty() = this is PropertyDescriptor && TypeUtils.isNullableType(type)

    private fun KtDeclarationWithBody.hasValidExternalBody(bindingContext: BindingContext): Boolean {
        if (!hasBody()) return true
        val body = bodyExpression!!
        return when {
            !hasBlockBody() -> body.isDefinedExternallyExpression(bindingContext)
            body is KtBlockExpression -> {
                val statement = body.statements.singleOrNull() ?: return false
                statement.isDefinedExternallyExpression(bindingContext)
            }
            else -> false
        }
    }

    private fun KtExpression.isDefinedExternallyExpression(bindingContext: BindingContext): Boolean {
        val descriptor = getResolvedCall(bindingContext)?.resultingDescriptor as? PropertyDescriptor ?: return false
        val container = descriptor.containingDeclaration as? PackageFragmentDescriptor ?: return false
        return DEFINED_EXTERNALLY_PROPERTY_NAMES.any { container.fqNameUnsafe == it.parent() && descriptor.name == it.shortName() }
    }
}
