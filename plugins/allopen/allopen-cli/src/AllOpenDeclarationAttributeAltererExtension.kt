/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.allopen

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.extensions.AnnotationBasedExtension
import org.jetbrains.kotlin.extensions.DeclarationAttributeAltererExtension
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.resolve.BindingContext

class CliAllOpenDeclarationAttributeAltererExtension(
        private val allOpenAnnotationFqNames: List<String>
) : AbstractAllOpenDeclarationAttributeAltererExtension() {
    override fun getAnnotationFqNames(modifierListOwner: KtModifierListOwner?) = allOpenAnnotationFqNames
}

abstract class AbstractAllOpenDeclarationAttributeAltererExtension : DeclarationAttributeAltererExtension, AnnotationBasedExtension {
    companion object {
        val ANNOTATIONS_FOR_TESTS = listOf("AllOpen", "AllOpen2", "test.AllOpen")
    }

    override fun refineDeclarationModality(
            modifierListOwner: KtModifierListOwner,
            declaration: DeclarationDescriptor?,
            containingDeclaration: DeclarationDescriptor?,
            currentModality: Modality,
            bindingContext: BindingContext
    ): Modality? {
        if (currentModality != Modality.FINAL) {
            return null
        }

        if (modifierListOwner.hasModifier(KtTokens.PRIVATE_KEYWORD) && modifierListOwner is KtCallableDeclaration) {
            return null
        }

        val descriptor = declaration as? ClassDescriptor ?: containingDeclaration ?: return null
        if (descriptor.hasSpecialAnnotation(modifierListOwner)) {
            return if (modifierListOwner.hasModifier(KtTokens.FINAL_KEYWORD))
                Modality.FINAL // Explicit final
            else
                Modality.OPEN
        }

        return null
    }
}