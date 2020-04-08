/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.allopen.ide

import org.jetbrains.kotlin.descriptors.*

/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiModifier
import org.jetbrains.kotlin.annotation.plugin.ide.CachedAnnotationNames
import org.jetbrains.kotlin.annotation.plugin.ide.getAnnotationNames
import org.jetbrains.kotlin.asJava.UltraLightClassModifierExtension
import org.jetbrains.kotlin.extensions.AnnotationBasedExtension
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.psiUtil.isPropertyParameter
import org.jetbrains.kotlin.util.isAnnotated
import org.jetbrains.kotlin.util.isOrdinaryClass

class AllOpenUltraLightClassModifierExtension(project: Project) :
    AnnotationBasedExtension,
    UltraLightClassModifierExtension {

    private val cachedAnnotationsNames = CachedAnnotationNames(project, ALL_OPEN_ANNOTATION_OPTION_PREFIX)

    override fun getAnnotationFqNames(modifierListOwner: KtModifierListOwner?): List<String> =
        cachedAnnotationsNames.getAnnotationNames(modifierListOwner)

    private val KtDeclaration.isMethodOrProperty get() = this is KtProperty || this is KtPropertyAccessor || this is KtFunction || (this is KtParameter && this.isPropertyParameter())

    private fun isSuitableDeclaration(declaration: KtDeclaration): Boolean {

        if (getAnnotationFqNames(declaration).isEmpty()) return false

        val declarationToCheck = if (declaration.isMethodOrProperty) declaration.containingClass() else declaration
        declarationToCheck ?: return false

        if (!declarationToCheck.isOrdinaryClass || declarationToCheck !is KtClassOrObject) return false

        if (declarationToCheck.superTypeListEntries.isEmpty() && !declarationToCheck.isAnnotated) return false

        return true
    }

    override fun interceptModalityBuilding(
        declaration: KtDeclaration,
        descriptor: Lazy<DeclarationDescriptor?>,
        modifier: String
    ): String {

        //Final can be altered to Open only
        if (modifier != PsiModifier.FINAL) return modifier

        if (!isSuitableDeclaration(declaration)) return modifier

        // Resolver will produce correct descriptor corresponding to modality from AllOpen.
        // The easiest way to get new modality is to resolve the descriptor
        return if ((descriptor.value as? MemberDescriptor)?.modality == Modality.OPEN) PsiModifier.OPEN else modifier
    }
}