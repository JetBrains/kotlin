/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.processor

import com.intellij.psi.PsiField
import com.intellij.psi.PsiModifier
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.lombok.config.LombokAnnotations.Data
import org.jetbrains.kotlin.lombok.config.LombokAnnotations.RequiredArgsConstructor
import org.jetbrains.kotlin.lombok.utils.LombokNames
import org.jetbrains.kotlin.lombok.utils.getJavaFields
import org.jetbrains.kotlin.resolve.source.getPsi

class RequiredArgsConstructorProcessor : AbstractConstructorProcessor<RequiredArgsConstructor>() {

    override fun getAnnotation(classDescriptor: ClassDescriptor): RequiredArgsConstructor? =
        RequiredArgsConstructor.getOrNull(classDescriptor) ?: Data.getOrNull(classDescriptor)?.asRequiredArgsConstructor()    

    override fun getPropertiesForParameters(classDescriptor: ClassDescriptor): List<PropertyDescriptor> =
        classDescriptor.getJavaFields().filter(this::isFieldRequired)

    private fun isFieldRequired(field: PropertyDescriptor): Boolean {
        val psi = field.source.getPsi()!! as PsiField

        val final = psi.modifierList?.hasModifierProperty(PsiModifier.FINAL) ?: false ||
                field.annotations.any { annotation -> LombokNames.NON_NULL_ANNOTATIONS.contains(annotation.fqName) }

        return final && !psi.hasInitializer()
    }

}
