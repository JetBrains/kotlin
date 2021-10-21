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
import org.jetbrains.kotlin.resolve.isInlineClass
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.isDynamic
import org.jetbrains.kotlin.types.typeUtil.*

object JsExportDeclarationChecker : DeclarationChecker {
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
                    INTERFACE -> if (descriptor.isExternal) null else "interface"
                    ANNOTATION_CLASS -> "annotation class"
                    CLASS -> if (descriptor.isInlineClass()) {
                        "${if (descriptor.isInline) "inline " else ""}${if (descriptor.isValue) "value " else ""}class"
                    } else null
                    else -> null
                }

                if (wrongDeclaration != null) {
                    reportWrongExportedDeclaration(wrongDeclaration)
                    return
                }

                if (descriptor.kind == ENUM_ENTRY) {
                    // Covered by ENUM_CLASS
                    return
                }

                for (superType in descriptor.defaultType.supertypes()) {
                    if (!superType.isExportable(bindingContext)) {
                        trace.report(ErrorsJs.NON_EXPORTABLE_TYPE.on(declaration, "super", superType))
                    }
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
                nonNullable.isDynamic() ||
                nonNullable.isBoolean() ||
                KotlinBuiltIns.isThrowableOrNullableThrowable(nonNullable) ||
                KotlinBuiltIns.isString(nonNullable) ||
                (nonNullable.isPrimitiveNumberOrNullableType() && !nonNullable.isLong()) ||
                nonNullable.isNothingOrNullableNothing() ||
                KotlinBuiltIns.isArray(this) ||
                KotlinBuiltIns.isPrimitiveArray(this)

        if (isPrimitiveExportableType) return true

        val descriptor = constructor.declarationDescriptor

        if (descriptor !is MemberDescriptor) return false

        return descriptor.isEffectivelyExternal() || AnnotationsUtils.isExportedObject(descriptor, bindingContext)
    }
}
