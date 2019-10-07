/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.noarg.ide

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.annotation.plugin.ide.CachedAnnotationNames
import org.jetbrains.kotlin.annotation.plugin.ide.getAnnotationNames
import org.jetbrains.kotlin.asJava.UltraLightClassCodegenSupport
import org.jetbrains.kotlin.asJava.classes.KtUltraLightClass
import org.jetbrains.kotlin.asJava.classes.createGeneratedMethodFromDescriptor
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.noarg.AbstractNoArgExpressionCodegenExtension
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.util.isAnnotated
import org.jetbrains.kotlin.util.isOrdinaryClass

class IdeNoArgExpressionCodegenExtension(project: Project) :
    AbstractNoArgExpressionCodegenExtension(invokeInitializers = false),
    UltraLightClassCodegenSupport {

    private val cachedAnnotationsNames = CachedAnnotationNames(project, NO_ARG_ANNOTATION_OPTION_PREFIX)

    override fun getAnnotationFqNames(modifierListOwner: KtModifierListOwner?): List<String> =
        cachedAnnotationsNames.getAnnotationNames(modifierListOwner)

    override fun interceptMethodsBuilding(
        declaration: KtDeclaration,
        descriptor: Lazy<DeclarationDescriptor?>,
        containingDeclaration: KtUltraLightClass,
        methodsList: MutableList<KtLightMethod>
    ) {
        if (!declaration.isOrdinaryClass || !declaration.isAnnotated) return

        if (cachedAnnotationsNames.getAnnotationNames(declaration).isEmpty()) return

        val descriptorValue = descriptor.value ?: return

        val classDescriptor = (descriptorValue as? ClassDescriptor)
            ?: descriptorValue.containingDeclaration as? ClassDescriptor
            ?: return

        if (!run { classDescriptor.hasSpecialAnnotation(declaration) }) return

        val parentClass = containingDeclaration as? KtUltraLightClass ?: return

        val constructorDescriptor = createNoArgConstructorDescriptor(classDescriptor)

        methodsList.add(parentClass.createGeneratedMethodFromDescriptor(constructorDescriptor))
    }
}
