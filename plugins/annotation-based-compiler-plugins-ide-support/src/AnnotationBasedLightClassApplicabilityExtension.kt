/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.annotation.plugin.ide

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.extensions.AnnotationBasedExtension
import org.jetbrains.kotlin.extensions.LightClassApplicabilityCheckExtension
import org.jetbrains.kotlin.extensions.LightClassApplicabilityType
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtModifierListOwner

abstract class AnnotationBasedLightClassApplicabilityExtension(project: Project, annotationOptionPrefix: String) :
    LightClassApplicabilityCheckExtension,
    AnnotationBasedExtension
{
    private val cachedAnnotationsNames = CachedAnnotationNames(project, annotationOptionPrefix)

    override fun getAnnotationFqNames(modifierListOwner: KtModifierListOwner?): List<String> =
        cachedAnnotationsNames.getAnnotationNames(modifierListOwner)

    override fun checkApplicabilityType(declaration: KtDeclaration, descriptor: Lazy<DeclarationDescriptor?>): LightClassApplicabilityType {
        if (!declaration.isOrdinaryClass || !declaration.isAnnotated) return LightClassApplicabilityType.UltraLightClass

        if (cachedAnnotationsNames.getAnnotationNames(declaration).isEmpty()) return LightClassApplicabilityType.UltraLightClass

        val descriptorValue = descriptor.value ?: return LightClassApplicabilityType.UltraLightClass

        val classDescriptor = (descriptorValue as? ClassDescriptor)
            ?: descriptorValue.containingDeclaration as? ClassDescriptor
            ?: return LightClassApplicabilityType.UltraLightClass

        val hasSpecialAnnotation = run { classDescriptor.hasSpecialAnnotation(declaration) }

        return if (hasSpecialAnnotation) LightClassApplicabilityType.LightClass else LightClassApplicabilityType.UltraLightClass
    }
}