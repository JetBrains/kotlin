/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.resolve.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyExternal
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isNothing
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.wasm.util.hasValidJsCodeBody

// TODO: Implement in K2: KT-56849
object WasmJsInteropTypesChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (descriptor !is MemberDescriptor)
            return

        val trace = context.trace
        val bindingContext = trace.bindingContext

        fun isExternalJsInteropDeclaration() =
            descriptor.isEffectivelyExternal() &&
                    !descriptor.annotations.hasAnnotation(FqName("kotlin.wasm.WasmImport"))

        fun isJsCodeDeclaration() =
            (descriptor is FunctionDescriptor && descriptor.hasValidJsCodeBody(bindingContext) ||
                    descriptor is PropertyDescriptor && descriptor.hasValidJsCodeBody(bindingContext))

        fun isJsExportDeclaration() =
            AnnotationsUtils.isExportedObject(descriptor, bindingContext)

        if (
            !isExternalJsInteropDeclaration() &&
            !isJsCodeDeclaration() &&
            !isJsExportDeclaration()
        ) {
            return
        }

        fun KotlinType.checkJsInteropType(
            typePositionDescription: String,
            reportOn: PsiElement,
            isInFunctionReturnPosition: Boolean = false,
        ) {
            if (!isTypeSupportedInJsInterop(this, isInFunctionReturnPosition)) {
                trace.report(ErrorsWasm.WRONG_JS_INTEROP_TYPE.on(reportOn, typePositionDescription, this))
            }
        }

        fun TypeParameterDescriptor.checkJsInteropTypeParameter() {
            for (upperBound in this.upperBounds) {
                if (!isTypeSupportedInJsInterop(upperBound, isInFunctionReturnPosition = false)) {
                    val reportOn = this.findPsi() ?: declaration
                    trace.report(ErrorsWasm.WRONG_JS_INTEROP_TYPE.on(reportOn, "type parameter upper bound", upperBound))
                }
            }
        }

        when (descriptor) {
            is ClassDescriptor -> {
                for (typeParameter in descriptor.declaredTypeParameters) {
                    typeParameter.checkJsInteropTypeParameter()
                }
            }
            is PropertyDescriptor -> {
                for (typeParameter in descriptor.typeParameters) {
                    typeParameter.checkJsInteropTypeParameter()
                }
                descriptor.type.checkJsInteropType("external property", declaration)
            }
            is FunctionDescriptor -> {
                for (typeParameter in descriptor.typeParameters) {
                    typeParameter.checkJsInteropTypeParameter()
                }
                for (parameter in descriptor.valueParameters) {
                    val typeToCheck = parameter.varargElementType ?: parameter.type
                    typeToCheck.checkJsInteropType(
                        "external function parameter",
                        reportOn = parameter.findPsi() ?: declaration
                    )
                }
                descriptor.returnType?.checkJsInteropType(
                    "external function return",
                    reportOn = declaration,
                    isInFunctionReturnPosition = true
                )
            }
        }
    }
}

private fun isTypeSupportedInJsInterop(
    type: KotlinType,
    isInFunctionReturnPosition: Boolean,
): Boolean {
    if (type.isUnit() || type.isNothing()) {
        return isInFunctionReturnPosition
    }

    val nonNullable = type.makeNotNullable()
    if (
        KotlinBuiltIns.isPrimitiveType(nonNullable) ||
        KotlinBuiltIns.isString(nonNullable)
    ) {
        return true
    }

    // Interop type parameters upper bounds should are checked
    // on declaration site separately
    if (nonNullable.isTypeParameter()) {
        return true
    }

    val classifierDescriptor = nonNullable.constructor.declarationDescriptor
    if (classifierDescriptor is MemberDescriptor && classifierDescriptor.isEffectivelyExternal()) {
        return true
    }

    if (type.isFunctionType) {
        val arguments = type.arguments
        for (i in 0 until arguments.lastIndex) {
            val isArgumentSupported = isTypeSupportedInJsInterop(
                arguments[i].type,
                isInFunctionReturnPosition = false,
            )

            if (!isArgumentSupported) {
                return false
            }
        }

        return isTypeSupportedInJsInterop(
            arguments.last().type,  // Function type result type
            isInFunctionReturnPosition = true,
        )
    }
    return false
}
