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
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.extensions.DeclarationAttributeAltererExtension
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.TypeUtils

class AllOpenDeclarationAttributeAltererExtension(val allOpenAnnotationFqNames: List<String>) : DeclarationAttributeAltererExtension {
    private companion object {
        private val INHERITED_FQNAME = "java.lang.annotation.Inherited"
    }

    override fun refineDeclarationModality(
            modifierListOwner: KtModifierListOwner,
            declaration: DeclarationDescriptor?,
            containingDeclaration: DeclarationDescriptor?,
            currentModality: Modality,
            bindingContext: BindingContext
    ): Modality? {
        // We alter only 'final' modality
        when (currentModality) {
            Modality.OPEN, Modality.ABSTRACT, Modality.SEALED -> return null
            else -> {}
        }

        // Explicit final
        if (modifierListOwner.hasModifier(KtTokens.FINAL_KEYWORD)) {
            return null
        }

        val descriptor = declaration ?: containingDeclaration ?: return null
        if (descriptor.hasAllOpenAnnotation()) return Modality.OPEN

        return null
    }

    private fun DeclarationDescriptor.hasAllOpenAnnotation(): Boolean {
        if (annotations.any { it.isAllOpenAnnotation() }) return true

        if (this is ClassDescriptor) {
            for (superType in TypeUtils.getAllSupertypes(defaultType)) {
                val superTypeDescriptor = superType.constructor.declarationDescriptor as? ClassDescriptor ?: continue
                if (superTypeDescriptor.annotations.any { it.isAllOpenAnnotation() }) return true
            }
        }

        return false
    }

    private fun AnnotationDescriptor.isAllOpenAnnotation(allowMetaAnnotations: Boolean = true): Boolean {
        val annotationType = type.constructor.declarationDescriptor ?: return false
        if (annotationType.fqNameSafe.asString() in allOpenAnnotationFqNames) return true

        if (allowMetaAnnotations) {
            for (metaAnnotation in annotationType.annotations) {
                if (metaAnnotation.isAllOpenAnnotation(allowMetaAnnotations = false)) {
                    return true
                }
            }
        }

        return false
    }
}