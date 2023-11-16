/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt4

import com.intellij.lang.jvm.JvmModifier
import com.intellij.psi.*
import com.intellij.psi.util.ClassUtil
import com.intellij.psi.util.PsiTypesUtil
import com.intellij.psi.util.PsiUtil


internal val PsiModifierListOwner.isPrivate: Boolean get() = hasModifier(JvmModifier.PRIVATE)
internal val PsiModifierListOwner.isFinal: Boolean get() = hasModifier(JvmModifier.FINAL)
internal val PsiModifierListOwner.isAbstract: Boolean get() = hasModifier(JvmModifier.ABSTRACT)
internal val PsiModifierListOwner.isStatic: Boolean get() = hasModifier(JvmModifier.STATIC)


internal val PsiMethod.signature: String
    get() = ClassUtil.getAsmMethodSignature(this)

internal val PsiField.signature: String
    get() = getAsmFieldSignature(this)

private fun getAsmFieldSignature(field: PsiField): String {
    return ClassUtil.getBinaryPresentation(field.type)
}

internal val PsiType.qualifiedName: String
    get() = qualifiedNameOrNull ?: canonicalText.replace("""<.*>""".toRegex(), "")

internal val PsiType.qualifiedNameOrNull: String?
    get() {
        if (this is PsiPrimitiveType) return name
        if (this is PsiWildcardType) return bound?.qualifiedNameOrNull
        return when (val resolvedClass = resolvedClass) {
            is PsiTypeParameter -> resolvedClass.name
            else -> resolvedClass?.qualifiedName
        }
    }

internal val PsiType.simpleNameOrNull: String?
    get() {
        if (this is PsiPrimitiveType) return name
        return when (val resolvedClass = resolvedClass) {
            is PsiTypeParameter -> resolvedClass.name
            else -> resolvedClass?.name
        }
    }

internal val PsiClass.defaultType: PsiType
    get() = PsiTypesUtil.getClassType(this)

internal val PsiType.resolvedClass: PsiClass?
    get() = (this as? PsiClassType)?.resolve()

internal val PsiClass.qualifiedNameWithDollars: String?
    get() {
        val packageName = PsiUtil.getPackageName(this) ?: return null
        if (packageName.isBlank()) {
            return qualifiedName?.replace(".", "$") ?: return null
        }

        val qualifiedName = this.qualifiedName ?: return null
        val className = qualifiedName.substringAfter("$packageName.")
        val classNameWithDollars = className.replace(".", "$")
        return "$packageName.$classNameWithDollars"
    }
