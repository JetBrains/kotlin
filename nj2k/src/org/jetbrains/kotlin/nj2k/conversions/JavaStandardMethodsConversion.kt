/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.impl.*

class JavaStandardMethodsConversion(private val context: NewJ2kConverterContext) : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKClass) return recurse(element)
        for (declaration in element.classBody.declarations) {
            if (declaration !is JKJavaMethodImpl) continue
            if (fixToStringMethod(declaration)) continue
            if (fixFinalizeMethod(declaration, element)) continue
            if (fixCloneMethod(declaration)) {
                val hasNoCloneableInSuperClasses =
                    declaration.psi<PsiMethod>()
                        ?.findSuperMethods()
                        ?.all { superMethod ->
                            superMethod.containingClass?.getKotlinFqName()?.asString() == "java.lang.Object"
                        } == true
                if (hasNoCloneableInSuperClasses) {
                    element.inheritance.implements +=
                        JKTypeElementImpl(
                            JKClassTypeImpl(
                                JKUnresolvedClassSymbol("Cloneable"),
                                emptyList(), Nullability.NotNull
                            )
                        )
                }
                continue
            }
        }
        return recurse(element)
    }

    private fun fixToStringMethod(method: JKJavaMethodImpl): Boolean {
        if (method.name.value != "toString") return false
        if (method.parameters.isNotEmpty()) return false
        val type = (method.returnType.type as? JKClassType)
            ?.takeIf { it.classReference.name == "String" }
            ?.updateNullability(Nullability.NotNull) ?: return false
        method.returnType = JKTypeElementImpl(type)
        return true
    }

    private fun fixCloneMethod(method: JKJavaMethodImpl): Boolean {
        if (method.name.value != "clone") return false
        if (method.parameters.isNotEmpty()) return false
        val type = (method.returnType.type as? JKClassType)
            ?.takeIf { it.classReference.name == "Object" }
            ?.updateNullability(Nullability.NotNull) ?: return false
        method.returnType = JKTypeElementImpl(type)
        return true
    }

    private fun fixFinalizeMethod(method: JKJavaMethodImpl, containingClass: JKClass): Boolean {
        if (method.name.value != "finalize") return false
        if (method.parameters.isNotEmpty()) return false
        if (method.returnType.type != JKJavaVoidType) return false
        if (method.hasOtherModifier(OtherModifier.OVERRIDE)) {
            method.modality =
                if (containingClass.modality == Modality.OPEN) Modality.OPEN
                else Modality.FINAL
            method.otherModifierElements -= method.otherModifierElements.first { it.otherModifier == OtherModifier.OVERRIDE }
        }
        return true
    }
}