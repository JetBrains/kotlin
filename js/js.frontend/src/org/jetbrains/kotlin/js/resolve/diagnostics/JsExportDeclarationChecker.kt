/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.resolve.diagnostics

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.ClassKind.*
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyExternal
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtensionProperty
import org.jetbrains.kotlin.resolve.inline.isInlineWithReified
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeProjection
import org.jetbrains.kotlin.types.isDynamic
import org.jetbrains.kotlin.types.typeUtil.*

object JsExportDeclarationChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        val trace = context.trace
        val bindingContext = trace.bindingContext

        fun checkTypeParameter(descriptor: TypeParameterDescriptor) {
            for (upperBound in descriptor.upperBounds) {
                if (!isTypeExportable(upperBound, bindingContext)) {
                    val typeParameterDeclaration = DescriptorToSourceUtils.descriptorToDeclaration(descriptor)!!
                    trace.report(ErrorsJs.NON_EXPORTABLE_TYPE.on(typeParameterDeclaration, "upper bound", upperBound))
                }
            }
        }

        fun checkValueParameter(descriptor: ValueParameterDescriptor) {
            if (!isTypeExportable(descriptor.type, bindingContext)) {
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
                        if (!isTypeExportable(returnType, bindingContext, true)) {
                            trace.report(ErrorsJs.NON_EXPORTABLE_TYPE.on(declaration, "return", returnType))
                        }
                    }
                }
            }

            is PropertyDescriptor -> {
                if (descriptor.isExtensionProperty) {
                    reportWrongExportedDeclaration("extension property")
                    return
                }
                if (!isTypeExportable(descriptor.type, bindingContext)) {
                    trace.report(ErrorsJs.NON_EXPORTABLE_TYPE.on(declaration, "property", descriptor.type))
                }
            }

            is ClassDescriptor -> {
                for (typeParameter in descriptor.declaredTypeParameters) {
                    checkTypeParameter(typeParameter)
                }

                if (descriptor.kind == ENUM_CLASS) {
                    reportWrongExportedDeclaration("enum class")
                    return
                }
                if (descriptor.kind == ANNOTATION_CLASS) {
                    reportWrongExportedDeclaration("annotation class")
                    return
                }
                if (descriptor.kind == ENUM_ENTRY) {
                    // Covered by ENUM_CLASS
                    return
                }

                for (superType in descriptor.defaultType.supertypes()) {
                    if (!isTypeExportable(superType, bindingContext)) {
                        trace.report(ErrorsJs.NON_EXPORTABLE_TYPE.on(declaration, "super", superType))
                    }
                }
            }
        }
    }


    private fun isTypeExportable(type: KotlinType, bindingContext: BindingContext, isReturnType: Boolean = false): Boolean {
        if (isReturnType && type.isUnit())
            return true

        if (type.isFunctionType) {
            val arguments = type.arguments
            val argumentsSize = type.arguments.size - 1
            for (i in 0 until argumentsSize) {
                if (!isTypeExportable(arguments[i].type, bindingContext))
                    return false
            }
            if (!isTypeExportable(arguments.last().type, bindingContext, isReturnType = true))
                return false

            return true
        }

        for (argument: TypeProjection in type.arguments) {
            if (!isTypeExportable(argument.type, bindingContext))
                return false
        }

        val nonNullable = type.makeNotNullable()

        // Is primitive exportable type
        if (nonNullable.isAnyOrNullableAny() ||
            nonNullable.isDynamic() ||
            nonNullable.isBoolean() ||
            KotlinBuiltIns.isThrowableOrNullableThrowable(nonNullable) ||
            KotlinBuiltIns.isString(nonNullable) ||
            (nonNullable.isPrimitiveNumberOrNullableType() && !nonNullable.isLong()) ||
            nonNullable.isNothingOrNullableNothing() ||
            (KotlinBuiltIns.isArray(type)) ||
            KotlinBuiltIns.isPrimitiveArray(type)
        ) return true

        val descriptor = type.constructor.declarationDescriptor ?: return false
        return descriptor is MemberDescriptor && descriptor.isEffectivelyExternal() ||
                AnnotationsUtils.isExportedObject(descriptor, bindingContext)
    }
}
