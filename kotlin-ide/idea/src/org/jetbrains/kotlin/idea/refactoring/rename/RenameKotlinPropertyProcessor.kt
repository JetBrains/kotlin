/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.rename

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReference
import com.intellij.psi.search.SearchScope
import com.intellij.refactoring.listeners.RefactoringElementListener
import com.intellij.refactoring.util.MoveRenameUsageInfo
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.propertyNameByAccessor
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.idea.core.isEnumCompanionPropertyWithEntryConflict
import org.jetbrains.kotlin.idea.core.unquote
import org.jetbrains.kotlin.idea.refactoring.dropOverrideKeywordIfNecessary
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.psiUtil.quoteIfNeeded
import org.jetbrains.kotlin.resolve.DataClassDescriptorResolver
import org.jetbrains.kotlin.resolve.DescriptorUtils

// FIX ME WHEN BUNCH 191 REMOVED
class RenameKotlinPropertyProcessor : RenameKotlinPropertyProcessorCompat() {

    override fun findReferences(
        element: PsiElement,
        searchScope: SearchScope,
        searchInCommentsAndStrings: Boolean
    ): Collection<PsiReference> {
        val references = super.findReferences(element, searchScope, searchInCommentsAndStrings)
        return processFoundReferences(element, references)
    }

    //TODO: a very long and complicated method, even recursive. mb refactor it somehow? at least split by PsiElement types?
    override tailrec fun renameElement(
        element: PsiElement,
        newName: String,
        usages: Array<UsageInfo>,
        listener: RefactoringElementListener?
    ) {
        val newNameUnquoted = newName.unquote()
        if (element is KtLightMethod) {
            if (element.modifierList.findAnnotation(DescriptorUtils.JVM_NAME.asString()) != null) {
                return super.renameElement(element, newName, usages, listener)
            }

            val origin = element.kotlinOrigin
            val newPropertyName = propertyNameByAccessor(newNameUnquoted, element)
            // Kotlin references to Kotlin property should not use accessor name
            if (newPropertyName != null && (origin is KtProperty || origin is KtParameter)) {
                val (ktUsages, otherUsages) = usages.partition { it.reference is KtSimpleNameReference }
                super.renameElement(element, newName, otherUsages.toTypedArray(), listener)
                renameElement(origin, newPropertyName.quoteIfNeeded(), ktUsages.toTypedArray(), listener)
                return
            }
        }

        if (element !is KtProperty && element !is KtParameter) {
            super.renameElement(element, newName, usages, listener)
            return
        }

        val name = (element as KtNamedDeclaration).name!!
        val oldGetterName = JvmAbi.getterName(name)
        val oldSetterName = JvmAbi.setterName(name)

        if (isEnumCompanionPropertyWithEntryConflict(element, newNameUnquoted)) {
            for ((i, usage) in usages.withIndex()) {
                if (usage !is MoveRenameUsageInfo) continue
                val ref = usage.reference ?: continue
                // TODO: Enum value can't be accessed from Java in case of conflict with companion member
                if (ref is KtReference) {
                    val newRef = (ref.bindToElement(element) as? KtSimpleNameExpression)?.mainReference ?: continue
                    usages[i] = MoveRenameUsageInfo(newRef, usage.referencedElement)
                }
            }
        }

        val adjustedUsages = if (element is KtParameter) usages.filterNot {
            val refTarget = it.reference?.resolve()
            refTarget is KtLightMethod && DataClassDescriptorResolver.isComponentLike(Name.guessByFirstCharacter(refTarget.name))
        } else usages.toList()

        val refKindUsages = adjustedUsages.groupBy { usage: UsageInfo ->
            val refElement = usage.reference?.resolve()
            if (refElement is PsiMethod) {
                val refElementName = refElement.name
                val refElementNameToCheck =
                    (if (usage is MangledJavaRefUsageInfo) KotlinTypeMapper.InternalNameMapper.demangleInternalName(refElementName) else null) ?: refElementName
                when (refElementNameToCheck) {
                    oldGetterName -> UsageKind.GETTER_USAGE
                    oldSetterName -> UsageKind.SETTER_USAGE
                    else -> UsageKind.SIMPLE_PROPERTY_USAGE
                }
            } else {
                UsageKind.SIMPLE_PROPERTY_USAGE
            }
        }

        super.renameElement(
            element.copy(), JvmAbi.setterName(newNameUnquoted).quoteIfNeeded(),
            refKindUsages[UsageKind.SETTER_USAGE]?.toTypedArray() ?: arrayOf<UsageInfo>(),
            null
        )

        super.renameElement(
            element.copy(), JvmAbi.getterName(newNameUnquoted).quoteIfNeeded(),
            refKindUsages[UsageKind.GETTER_USAGE]?.toTypedArray() ?: arrayOf<UsageInfo>(),
            null
        )

        super.renameElement(
            element, newName,
            refKindUsages[UsageKind.SIMPLE_PROPERTY_USAGE]?.toTypedArray() ?: arrayOf<UsageInfo>(),
            null
        )

        usages.forEach { (it as? KtResolvableCollisionUsageInfo)?.apply() }

        dropOverrideKeywordIfNecessary(element)

        listener?.elementRenamed(element)
    }

}
