/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.noarg.ide

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.annotation.plugin.ide.CachedAnnotationNames
import org.jetbrains.kotlin.annotation.plugin.ide.getAnnotationNames
import org.jetbrains.kotlin.asJava.UltraLightClassModifierExtension
import org.jetbrains.kotlin.asJava.classes.KtUltraLightClass
import org.jetbrains.kotlin.asJava.classes.createGeneratedMethodFromDescriptor
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.extensions.AnnotationBasedExtension
import org.jetbrains.kotlin.noarg.AbstractNoArgExpressionCodegenExtension
import org.jetbrains.kotlin.noarg.AbstractNoArgExpressionCodegenExtension.Companion.isZeroParameterConstructor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.util.isAnnotated
import org.jetbrains.kotlin.util.isOrdinaryClass

class NoArgUltraLightClassModifierExtension(project: Project) :
    AnnotationBasedExtension,
    UltraLightClassModifierExtension {

    private val cachedAnnotationsNames = CachedAnnotationNames(project, NO_ARG_ANNOTATION_OPTION_PREFIX)

    override fun getAnnotationFqNames(modifierListOwner: KtModifierListOwner?): List<String> =
        cachedAnnotationsNames.getAnnotationNames(modifierListOwner)

    private fun isSuitableDeclaration(declaration: KtDeclaration): Boolean {
        if (getAnnotationFqNames(declaration).isEmpty()) return false

        if (!declaration.isOrdinaryClass || declaration !is KtClassOrObject) return false

        if (declaration.allConstructors.isEmpty()) return false

        if (declaration.allConstructors.any { it.getValueParameters().isEmpty() }) return false

        if (declaration.superTypeListEntries.isEmpty() && !declaration.isAnnotated) return false

        return true
    }

    override fun interceptMethodsBuilding(
        declaration: KtDeclaration,
        descriptor: Lazy<DeclarationDescriptor?>,
        containingDeclaration: KtUltraLightClass,
        methodsList: MutableList<KtLightMethod>
    ) {
        val parentClass = containingDeclaration as? KtUltraLightClass ?: return

        if (methodsList.any { it.isConstructor && it.parameters.isEmpty() }) return

        if (!isSuitableDeclaration(declaration)) return

        val descriptorValue = descriptor.value ?: return

        val classDescriptor = (descriptorValue as? ClassDescriptor)
            ?: descriptorValue.containingDeclaration as? ClassDescriptor
            ?: return

        if (!classDescriptor.hasSpecialAnnotation(declaration)) return

        if (classDescriptor.constructors.any { isZeroParameterConstructor(it) }) return

        val constructorDescriptor = AbstractNoArgExpressionCodegenExtension.createNoArgConstructorDescriptor(classDescriptor)

        methodsList.add(parentClass.createGeneratedMethodFromDescriptor(constructorDescriptor))
    }
}