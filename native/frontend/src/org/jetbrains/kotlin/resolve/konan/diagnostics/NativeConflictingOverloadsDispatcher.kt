/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.konan.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory1
import org.jetbrains.kotlin.ir.objcinterop.getObjCMethodInfo
import org.jetbrains.kotlin.name.NativeStandardInteropNames.Annotations.objCSignatureOverrideClassId
import org.jetbrains.kotlin.resolve.ConflictingOverloadsDispatcher

/**
 * This function basically checks that these two functions have different objective-C signature.
 *
 * This signature consists of function name and parameter names except first.
 *
 * So we ignore the first parameter name, but check others
 */
private fun FunctionDescriptor.hasDifferentParameterNames(other: FunctionDescriptor) : Boolean {
    return valueParameters.drop(1).map { it.name } != other.valueParameters.drop(1).map { it.name }
}

object NativeConflictingOverloadsDispatcher : ConflictingOverloadsDispatcher {
    override fun getDiagnostic(
        languageVersionSettings: LanguageVersionSettings,
        declaration: DeclarationDescriptor,
        redeclarations: Collection<DeclarationDescriptor>
    ): DiagnosticFactory1<PsiElement, Collection<DeclarationDescriptor>>? {
        if (languageVersionSettings.supportsFeature(LanguageFeature.ObjCSignatureOverrideAnnotation)) {
            if (declaration is FunctionDescriptor && redeclarations.all { it is FunctionDescriptor }) {
                if (declaration.getObjCMethodInfo() != null && redeclarations.all { (it as FunctionDescriptor).getObjCMethodInfo() != null }) {
                    if (redeclarations.all { it === declaration || (it as FunctionDescriptor).hasDifferentParameterNames(declaration) }) {
                        if (declaration.annotations.hasAnnotation(objCSignatureOverrideClassId.asSingleFqName())) {
                            return null
                        } else {
                            return ErrorsNative.CONFLICTING_OBJC_OVERLOADS
                        }
                    }
                }
            }
        }
        return ConflictingOverloadsDispatcher.Default.getDiagnostic(languageVersionSettings, declaration, redeclarations)
    }
}