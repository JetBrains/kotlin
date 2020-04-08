/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.PsiModificationTracker
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.*
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.isOverridable
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.core.util.CachedValue
import org.jetbrains.kotlin.idea.core.util.getValue
import org.jetbrains.kotlin.idea.refactoring.canRefactor
import org.jetbrains.kotlin.idea.util.actualsForExpected
import org.jetbrains.kotlin.idea.util.isExpectDeclaration
import org.jetbrains.kotlin.lexer.KtTokens.OPEN_KEYWORD
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

private typealias DeclarationPointer = SmartPsiElementPointer<KtCallableDeclaration>

class MakeOverriddenMemberOpenFix(declaration: KtDeclaration) : KotlinQuickFixAction<KtDeclaration>(declaration) {

    private val myQuickFixInfo: QuickFixInfo by CachedValue(declaration.project) {
        CachedValueProvider.Result.createSingleDependency(computeInfo(), PsiModificationTracker.MODIFICATION_COUNT)
    }

    private val containingDeclarationsNames
        get() = myQuickFixInfo.declNames

    private val overriddenNonOverridableMembers
        get() = myQuickFixInfo.declarations

    private fun computeInfo(): QuickFixInfo {
        val element = element ?: return QUICKFIX_UNAVAILABLE
        val overriddenNonOverridableMembers = mutableListOf<DeclarationPointer>()
        val containingDeclarationsNames = mutableListOf<String>()
        val descriptor = element.resolveToDescriptorIfAny(BodyResolveMode.FULL) as? CallableMemberDescriptor ?: return QUICKFIX_UNAVAILABLE

        for (overriddenDescriptor in getAllDeclaredNonOverridableOverriddenDescriptors(descriptor)) {
            assert(overriddenDescriptor.kind == DECLARATION) { "Can only be applied to declarations." }
            val overriddenMember = DescriptorToSourceUtils.descriptorToDeclaration(overriddenDescriptor)
            if (overriddenMember == null || !overriddenMember.canRefactor() || overriddenMember !is KtCallableDeclaration ||
                overriddenMember.modifierList?.hasModifier(OPEN_KEYWORD) == true
            ) {
                return QUICKFIX_UNAVAILABLE
            }

            overriddenDescriptor.takeIf { overriddenMember.isExpectDeclaration() }?.actualsForExpected()?.forEach {
                if (it is MemberDescriptor && it.modality < Modality.OPEN) {
                    val member = DescriptorToSourceUtils.descriptorToDeclaration(it)
                    if (member == null || !member.canRefactor() || member !is KtCallableDeclaration) {
                        return QUICKFIX_UNAVAILABLE
                    }
                    overriddenNonOverridableMembers.add(member.createSmartPointer())
                }
            }

            val containingDeclarationName = overriddenDescriptor.containingDeclaration.name.asString()
            overriddenNonOverridableMembers.add(overriddenMember.createSmartPointer())
            containingDeclarationsNames.add(containingDeclarationName)
        }
        return QuickFixInfo(overriddenNonOverridableMembers, containingDeclarationsNames)
    }

    override fun isAvailable(project: Project, editor: Editor?, file: KtFile): Boolean {
        return overriddenNonOverridableMembers.isNotEmpty()
    }

    override fun getText(): String {
        val element = element ?: return ""
        if (containingDeclarationsNames.size == 1) {
            val name = containingDeclarationsNames[0] + "." + element.name
            return KotlinBundle.message("make.0", "$name $OPEN_KEYWORD")
        }
        val sortedDeclarationNames = containingDeclarationsNames.sorted()
        val declarations = sortedDeclarationNames.subList(0, sortedDeclarationNames.size - 1).joinToString(", ") +
                " ${KotlinBundle.message("configuration.text.and")} " +
                sortedDeclarationNames.last()
        return KotlinBundle.message("make.0.in.1.open", element.name.toString(), declarations)
    }

    override fun getFamilyName(): String = KotlinBundle.message("add.modifier")

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        for (overriddenMember in overriddenNonOverridableMembers) {
            overriddenMember.element?.addModifier(OPEN_KEYWORD)
        }
    }

    companion object : KotlinSingleIntentionActionFactory() {
        private data class QuickFixInfo(val declarations: List<DeclarationPointer>, val declNames: List<String>)

        private val QUICKFIX_UNAVAILABLE = QuickFixInfo(emptyList(), emptyList())

        private fun getAllDeclaredNonOverridableOverriddenDescriptors(
            callableMemberDescriptor: CallableMemberDescriptor
        ): Collection<CallableMemberDescriptor> {
            val result = hashSetOf<CallableMemberDescriptor>()
            val nonOverridableOverriddenDescriptors = retainNonOverridableMembers(callableMemberDescriptor.overriddenDescriptors)
            for (overriddenDescriptor in nonOverridableOverriddenDescriptors) {
                when (overriddenDescriptor.kind) {
                    DECLARATION ->
                        result.add(overriddenDescriptor)

                    FAKE_OVERRIDE, DELEGATION ->
                        result.addAll(getAllDeclaredNonOverridableOverriddenDescriptors(overriddenDescriptor))

                    SYNTHESIZED -> {
                    } /* do nothing */

                    else -> throw UnsupportedOperationException("Unexpected callable kind ${overriddenDescriptor.kind}")
                }
            }
            return result
        }

        private fun retainNonOverridableMembers(
            callableMemberDescriptors: Collection<CallableMemberDescriptor>
        ): Collection<CallableMemberDescriptor> {
            return callableMemberDescriptors.filter { !it.isOverridable }
        }

        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val declaration = diagnostic.psiElement.getNonStrictParentOfType<KtDeclaration>()!!
            return MakeOverriddenMemberOpenFix(declaration)
        }
    }
}
