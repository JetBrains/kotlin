/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDocCommentOwner
import com.intellij.psi.PsiNamedElement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.core.completion.DeclarationLookupObject
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.deprecation.getSinceVersion
import org.jetbrains.kotlin.util.descriptorsEqualWithSubstitution

/**
 * Stores information about resolved descriptor and position of that descriptor.
 * Position will be used for sorting
 */
abstract class DeclarationLookupObjectImpl(
    final override val descriptor: DeclarationDescriptor?
) : DeclarationLookupObject {
    override val name: Name?
        get() = descriptor?.name ?: (psiElement as? PsiNamedElement)?.name?.let { Name.identifier(it) }

    override val importableFqName: FqName?
        get() {
            return if (descriptor != null)
                descriptor.importableFqName
            else
                (psiElement as? PsiClass)?.qualifiedName?.let(::FqName)
        }

    override fun toString() = super.toString() + " " + (descriptor ?: psiElement)

    override fun hashCode(): Int {
        return if (descriptor != null)
            descriptor.original.hashCode()
        else
            psiElement!!.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class.java != other::class.java) return false
        val lookupObject = other as DeclarationLookupObjectImpl
        return if (descriptor != null)
            descriptorsEqualWithSubstitution(descriptor, lookupObject.descriptor)
        else
            lookupObject.descriptor == null && psiElement == lookupObject.psiElement
    }

    override val isDeprecated: Boolean
        get() {
            return if (descriptor != null)
                isDeprecatedAtCallSite(descriptor) { psiElement?.languageVersionSettings }
            else
                (psiElement as? PsiDocCommentOwner)?.isDeprecated == true
        }

}

// This function is kind of a hack to avoid using DeprecationResolver as it's hard to preserve same resolutionFacade for descriptor
fun isDeprecatedAtCallSite(descriptor: DeclarationDescriptor, languageVersionSettings: () -> LanguageVersionSettings?): Boolean {
    if (!KotlinBuiltIns.isDeprecated(descriptor)) return false

    val annotation = descriptor.original.annotations.findAnnotation(StandardNames.FqNames.deprecatedSinceKotlin) ?: return true

    //only from here we probably need languageVersionSettings, which evaluation could be costly
    val hiddenSince = annotation.getSinceVersion("hiddenSince")
    val errorSince = annotation.getSinceVersion("errorSince")
    val warningSince = annotation.getSinceVersion("warningSince")

    if (hiddenSince == null && errorSince == null && warningSince == null) {
        return false //actually shouldn't happen, was false before refactoring
    }
    val apiVersion = languageVersionSettings()?.apiVersion ?: return true
    if (hiddenSince != null && apiVersion >= hiddenSince) return true
    if (errorSince != null && apiVersion >= errorSince) return true
    if (warningSince != null && apiVersion >= warningSince) return true

    return false
}
