/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.resolve.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.ClassKind.*
import org.jetbrains.kotlin.js.common.RESERVED_KEYWORDS
import org.jetbrains.kotlin.js.common.SPECIAL_KEYWORDS
import org.jetbrains.kotlin.js.naming.NameSuggestion
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyExternal
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtensionProperty
import org.jetbrains.kotlin.resolve.descriptorUtil.isInsideInterface
import org.jetbrains.kotlin.resolve.inline.isInlineWithReified
import org.jetbrains.kotlin.resolve.isInlineClass
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.isDynamic
import org.jetbrains.kotlin.types.typeUtil.*

class JsExportDeclarationChecker(private val includeUnsignedNumbers: Boolean) : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        val trace = context.trace
        val bindingContext = trace.bindingContext

        fun checkTypeParameter(descriptor: TypeParameterDescriptor) {
            for (upperBound in descriptor.upperBounds) {
                if (!upperBound.isExportable(bindingContext)) {
                    val typeParameterDeclaration = DescriptorToSourceUtils.descriptorToDeclaration(descriptor)!!
                    trace.report(ErrorsJs.NON_EXPORTABLE_TYPE.on(typeParameterDeclaration, "upper bound", upperBound))
                }
            }
        }

        fun checkValueParameter(descriptor: ValueParameterDescriptor) {
            if (!descriptor.type.isExportable(bindingContext)) {
                val valueParameterDeclaration = DescriptorToSourceUtils.descriptorToDeclaration(descriptor)!!
                trace.report(ErrorsJs.NON_EXPORTABLE_TYPE.on(valueParameterDeclaration, "parameter", descriptor.type))
            }
        }

        if (!AnnotationsUtils.isExportedObject(descriptor, bindingContext)) return
        if (descriptor !is MemberDescriptor)
            return

        val hasJsName = AnnotationsUtils.getJsNameAnnotation(descriptor) != null

        fun reportWrongExportedDeclaration(kind: String) {
            trace.report(ErrorsJs.WRONG_EXPORTED_DECLARATION.on(declaration, kind))
        }

        if (descriptor.isExpect) {
            reportWrongExportedDeclaration("expect")
        }

        validateDeclarationOnConsumableName(declaration, descriptor, trace)

        when (descriptor) {
            is FunctionDescriptor -> {
                for (typeParameter in descriptor.typeParameters) {
                    checkTypeParameter(typeParameter)
                }

                if (descriptor.isInlineWithReified()) {
                    reportWrongExportedDeclaration("inline function with reified type parameters")
                    return
                }

                if (descriptor.isSuspend) {
                    reportWrongExportedDeclaration("suspend function")
                    return
                }

                if (descriptor is ConstructorDescriptor) {
                    if (!descriptor.isPrimary && !hasJsName)
                        reportWrongExportedDeclaration("secondary constructor without @JsName")
                }

                // Properties are checked instead of property accessors
                if (descriptor !is PropertyAccessorDescriptor) {
                    for (parameter in descriptor.valueParameters) {
                        checkValueParameter(parameter)
                    }

                    descriptor.returnType?.let { returnType ->
                        if (!returnType.isExportableReturn(bindingContext)) {
                            trace.report(ErrorsJs.NON_EXPORTABLE_TYPE.on(declaration, "return", returnType))
                        }
                    }
                }
            }

            is PropertyDescriptor -> {
                if (declaration is KtParameter) return
                if (descriptor.isExtensionProperty) {
                    reportWrongExportedDeclaration("extension property")
                    return
                }
                if (!descriptor.type.isExportable(bindingContext)) {
                    trace.report(ErrorsJs.NON_EXPORTABLE_TYPE.on(declaration, "property", descriptor.type))
                }
            }

            is ClassDescriptor -> {
                for (typeParameter in descriptor.declaredTypeParameters) {
                    checkTypeParameter(typeParameter)
                }

                val wrongDeclaration: String? = when (descriptor.kind) {
                    ANNOTATION_CLASS -> "annotation class"
                    CLASS -> when {
                        descriptor.isInsideInterface -> "nested class inside exported interface"
                        descriptor.isInlineClass() -> "${if (descriptor.isInline) "inline " else ""}${if (descriptor.isValue) "value " else ""}class"
                        else -> null
                    }
                    else -> if (descriptor.isInsideInterface) {
                        "${if (descriptor.isCompanionObject) "companion object" else "nested/inner declaration"} inside exported interface"
                    } else null
                }

                if (wrongDeclaration != null) {
                    reportWrongExportedDeclaration(wrongDeclaration)
                    return
                }

                if (descriptor.kind == ENUM_ENTRY) {
                    // Covered by ENUM_CLASS
                    return
                }
            }
        }
    }

    private fun KotlinType.isExportableReturn(bindingContext: BindingContext, currentlyProcessed: MutableSet<KotlinType> = mutableSetOf()) =
        isUnit() || isExportable(bindingContext, currentlyProcessed)

    private fun KotlinType.isExportable(
        bindingContext: BindingContext,
        currentlyProcessed: MutableSet<KotlinType> = mutableSetOf()
    ): Boolean {
        if (!currentlyProcessed.add(this)) {
            return true
        }

        currentlyProcessed.add(this)

        if (isFunctionType) {
            for (i in 0 until arguments.lastIndex) {
                if (!arguments[i].type.isExportable(bindingContext, currentlyProcessed)) {
                    currentlyProcessed.remove(this)
                    return false
                }
            }

            currentlyProcessed.remove(this)
            return arguments.last().type.isExportableReturn(bindingContext, currentlyProcessed)
        }

        for (argument in arguments) {
            if (!argument.type.isExportable(bindingContext, currentlyProcessed)) {
                currentlyProcessed.remove(this)
                return false
            }
        }

        currentlyProcessed.remove(this)

        val nonNullable = makeNotNullable()

        val isPrimitiveExportableType = nonNullable.isAnyOrNullableAny() ||
                nonNullable.isTypeParameter() ||
                nonNullable.isDynamic() ||
                nonNullable.isBoolean() ||
                KotlinBuiltIns.isThrowableOrNullableThrowable(nonNullable) ||
                KotlinBuiltIns.isString(nonNullable) ||
                (nonNullable.isPrimitiveNumberOrNullableType() && !nonNullable.isLong()) ||
                nonNullable.isNothingOrNullableNothing() ||
                (includeUnsignedNumbers && KotlinBuiltIns.isUnsignedNumber(nonNullable)) ||
                KotlinBuiltIns.isArray(this) ||
                KotlinBuiltIns.isPrimitiveArray(this) ||
                KotlinBuiltIns.isConstructedFromGivenClass(this, StandardNames.FqNames.list) ||
                KotlinBuiltIns.isConstructedFromGivenClass(this, StandardNames.FqNames.mutableList) ||
                KotlinBuiltIns.isConstructedFromGivenClass(this, StandardNames.FqNames.set) ||
                KotlinBuiltIns.isConstructedFromGivenClass(this, StandardNames.FqNames.mutableSet) ||
                KotlinBuiltIns.isConstructedFromGivenClass(this, StandardNames.FqNames.map) ||
                KotlinBuiltIns.isConstructedFromGivenClass(this, StandardNames.FqNames.mutableMap)

        if (isPrimitiveExportableType) return true

        val descriptor = constructor.declarationDescriptor

        if (descriptor !is MemberDescriptor) return false

        if (KotlinBuiltIns.isEnum(this)) return true

        return descriptor.isEffectivelyExternal() || AnnotationsUtils.isExportedObject(descriptor, bindingContext)
    }

    private fun validateDeclarationOnConsumableName(
        declaration: KtDeclaration,
        declarationDescriptor: DeclarationDescriptor,
        trace: BindingTrace
    ) {
        if (!declarationDescriptor.isTopLevelInPackage() || declarationDescriptor.name.isSpecial) return

        val name = declarationDescriptor.getKotlinOrJsName()

        if (name in SPECIAL_KEYWORDS || (name !in RESERVED_KEYWORDS && NameSuggestion.sanitizeName(name) == name)) return

        val reportTarget = declarationDescriptor.getJsNameArgument() ?: declaration.getIdentifier()

        trace.report(ErrorsJs.NON_CONSUMABLE_EXPORTED_IDENTIFIER.on(reportTarget, name))
    }

    private fun DeclarationDescriptor.getKotlinOrJsName(): String {
        return AnnotationsUtils.getJsName(this) ?: name.identifier
    }

    private fun KtDeclaration.getIdentifier(): PsiElement {
        return (this as KtNamedDeclaration).nameIdentifier!!
    }

    private fun DeclarationDescriptor.getJsNameArgument(): PsiElement? {
        val jsNameAnnotation = AnnotationsUtils.getJsNameAnnotation(this) ?: return null
        return (jsNameAnnotation.source.getPsi() as KtAnnotationEntry).valueArgumentList?.arguments?.first()
    }
}
