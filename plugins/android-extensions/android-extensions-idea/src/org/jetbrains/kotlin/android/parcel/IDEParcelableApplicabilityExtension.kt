/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.android.parcel

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.extensions.LightClassApplicabilityCheckExtension
import org.jetbrains.kotlin.extensions.LightClassApplicabilityType
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.extensions.LightClassApplicabilityType.LightClass
import org.jetbrains.kotlin.extensions.LightClassApplicabilityType.UltraLightClass
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass

private val KtDeclaration.isOrdinaryClass
    get() = this is KtClass &&
            !this.hasModifier(KtTokens.INLINE_KEYWORD) &&
            !this.isAnnotation() &&
            !this.isInterface()

private val KtDeclaration.isAnnotated get() = this.annotationEntries.isNotEmpty()

class IDEParcelableApplicabilityExtension : LightClassApplicabilityCheckExtension {

    override fun checkApplicabilityType(declaration: KtDeclaration, descriptor: Lazy<DeclarationDescriptor?>): LightClassApplicabilityType {

        if (!declaration.isOrdinaryClass || !declaration.isAnnotated) return UltraLightClass

        val descriptorValue = descriptor.value ?: return UltraLightClass

        val classDescriptor = (descriptorValue as? ClassDescriptor)
            ?: descriptorValue.containingDeclaration as? ClassDescriptor
            ?: return UltraLightClass

        return if (classDescriptor.isParcelize) LightClass else UltraLightClass
    }
}