/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.lang.jvm.JvmModifier
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isAbstract
import org.jetbrains.kotlin.psi.psiUtil.isExtensionDeclaration
import org.jetbrains.kotlin.idea.highlighter.KotlinHighlightingColors as Colors

internal fun textAttributesKeyForPropertyDeclaration(declaration: PsiElement): TextAttributesKey? = when (declaration) {
    is KtProperty -> textAttributesForKtPropertyDeclaration(declaration)
    is KtParameter -> textAttributesForKtParameterDeclaration(declaration)
    is PsiLocalVariable -> Colors.LOCAL_VARIABLE
    is PsiParameter -> Colors.PARAMETER
    is PsiField -> Colors.INSTANCE_PROPERTY
    else -> null
}

internal fun textAttributesForKtParameterDeclaration(parameter: KtParameter) =
    if (parameter.valOrVarKeyword != null) Colors.INSTANCE_PROPERTY
    else Colors.PARAMETER

internal fun textAttributesForKtPropertyDeclaration(property: KtProperty): TextAttributesKey? = when {
    property.isExtensionDeclaration() -> Colors.EXTENSION_PROPERTY
    property.isLocal -> Colors.LOCAL_VARIABLE
    property.isTopLevel -> {
        if (property.isCustomPropertyDeclaration()) Colors.PACKAGE_PROPERTY_CUSTOM_PROPERTY_DECLARATION
        else Colors.PACKAGE_PROPERTY
    }
    else -> {
        if (property.isCustomPropertyDeclaration()) Colors.INSTANCE_PROPERTY_CUSTOM_PROPERTY_DECLARATION
        else Colors.INSTANCE_PROPERTY
    }
}

private fun KtProperty.isCustomPropertyDeclaration() =
    getter?.bodyExpression != null || setter?.bodyExpression != null

@Suppress("UnstableApiUsage")
internal fun textAttributesKeyForTypeDeclaration(declaration: PsiElement): TextAttributesKey? = when {
    declaration is KtTypeParameter || declaration is PsiTypeParameter -> Colors.TYPE_PARAMETER
    declaration is KtTypeAlias -> Colors.TYPE_ALIAS
    declaration is KtClass -> textAttributesForClass(declaration)
    declaration is PsiClass && declaration.isInterface && !declaration.isAnnotationType -> Colors.TRAIT
    declaration.isAnnotationClass() -> Colors.ANNOTATION
    declaration is KtObjectDeclaration -> Colors.OBJECT
    declaration is PsiEnumConstant -> Colors.ENUM_ENTRY
    declaration is PsiClass && declaration.hasModifier(JvmModifier.ABSTRACT) -> Colors.ABSTRACT_CLASS
    declaration is PsiClass -> Colors.CLASS
    else -> null
}

fun textAttributesForClass(klass: KtClass): TextAttributesKey = when {
    klass.isInterface() -> Colors.TRAIT
    klass.isAnnotation() -> Colors.ANNOTATION
    klass is KtEnumEntry -> Colors.ENUM_ENTRY
    klass.isAbstract() -> Colors.ABSTRACT_CLASS
    else -> Colors.CLASS
}

internal fun PsiElement.isAnnotationClass() =
    this is KtClass && isAnnotation() || this is PsiClass && isAnnotationType