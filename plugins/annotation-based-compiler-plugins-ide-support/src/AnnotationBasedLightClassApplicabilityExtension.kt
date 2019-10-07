/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.annotation.plugin.ide

import org.jetbrains.kotlin.util.isAnnotated
import org.jetbrains.kotlin.util.isOrdinaryClass
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.extensions.AnnotationBasedExtension
import org.jetbrains.kotlin.extensions.LightClassApplicabilityCheckExtension
import org.jetbrains.kotlin.extensions.LightClassApplicabilityType
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.extensions.LightClassApplicabilityType.LightClass
import org.jetbrains.kotlin.extensions.LightClassApplicabilityType.UltraLightClass

abstract class AnnotationBasedLightClassApplicabilityExtension(project: Project, annotationOptionPrefix: String) :
    LightClassApplicabilityCheckExtension,
    AnnotationBasedExtension
{
    private val cachedAnnotationsNames = CachedAnnotationNames(project, annotationOptionPrefix)

    override fun getAnnotationFqNames(modifierListOwner: KtModifierListOwner?): List<String> =
        cachedAnnotationsNames.getAnnotationNames(modifierListOwner)

    override fun checkApplicabilityType(declaration: KtDeclaration, descriptor: Lazy<DeclarationDescriptor?>): LightClassApplicabilityType {

        return UltraLightClass
//
//        if (!declaration.isOrdinaryClass || !declaration.isAnnotated) return UltraLightClass
//
//        if (cachedAnnotationsNames.getAnnotationNames(declaration).isEmpty()) return UltraLightClass
//
//        val descriptorValue = descriptor.value ?: return UltraLightClass
//
//        val classDescriptor = (descriptorValue as? ClassDescriptor)
//            ?: descriptorValue.containingDeclaration as? ClassDescriptor
//            ?: return UltraLightClass
//
//        val hasSpecialAnnotation = run { classDescriptor.hasSpecialAnnotation(declaration) }
//
//        return if (hasSpecialAnnotation) LightClass else UltraLightClass
    }
}