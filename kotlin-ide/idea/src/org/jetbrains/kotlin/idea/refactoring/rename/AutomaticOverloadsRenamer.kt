/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.rename

import com.intellij.java.refactoring.JavaRefactoringBundle
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.refactoring.rename.naming.AutomaticRenamer
import com.intellij.refactoring.rename.naming.AutomaticRenamerFactory
import com.intellij.usageView.UsageInfo
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.refactoring.KotlinRefactoringSettings
import org.jetbrains.kotlin.idea.util.expectedDescriptor
import org.jetbrains.kotlin.idea.util.getAllAccessibleFunctions
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.UserDataProperty
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.source.getPsi

class AutomaticOverloadsRenamer(function: KtNamedFunction, newName: String) : AutomaticRenamer() {
    companion object {
        @get:TestOnly
        @set:TestOnly
        internal var PsiElement.elementFilter: ((PsiElement) -> Boolean)? by UserDataProperty(Key.create("ELEMENT_FILTER"))
    }

    init {
        val filter = function.elementFilter
        function.getOverloads().mapNotNullTo(myElements) {
            val candidate = it.source.getPsi() as? KtNamedFunction ?: return@mapNotNullTo null
            if (filter != null && !filter(candidate)) return@mapNotNullTo null
            if (candidate != function) candidate else null
        }
        suggestAllNames(function.name, newName)
    }

    override fun getDialogTitle() = KotlinBundle.message("text.rename.overloads.title")
    override fun getDialogDescription() = KotlinBundle.message("text.rename.overloads.to")
    override fun entityName() = KotlinBundle.message("text.overload")
    override fun isSelectedByDefault(): Boolean = true
}

private fun KtNamedFunction.getOverloads(): Collection<FunctionDescriptor> {
    val name = nameAsName ?: return emptyList()
    val resolutionFacade = getResolutionFacade()
    val descriptor = this.unsafeResolveToDescriptor() as FunctionDescriptor
    val context = resolutionFacade.analyze(this, BodyResolveMode.FULL)
    val scope = getResolutionScope(context, resolutionFacade)
    val extensionReceiverClass = descriptor.extensionReceiverParameter?.type?.constructor?.declarationDescriptor as? ClassDescriptor

    if (descriptor.isActual && descriptor.expectedDescriptor() != null) return emptyList()

    val result = LinkedHashSet<FunctionDescriptor>()
    result += scope.getAllAccessibleFunctions(name)
    if (extensionReceiverClass != null) {
        result += extensionReceiverClass.unsubstitutedMemberScope.getContributedFunctions(name, NoLookupLocation.FROM_IDE)
    }

    return result
}

class AutomaticOverloadsRenamerFactory : AutomaticRenamerFactory {
    override fun isApplicable(element: PsiElement): Boolean {
        if (element !is KtNamedFunction) return false
        if (element.isLocal) return false
        return element.getOverloads().size > 1
    }

    override fun getOptionName() = JavaRefactoringBundle.message("rename.overloads")

    override fun isEnabled() = KotlinRefactoringSettings.instance.renameOverloads

    override fun setEnabled(enabled: Boolean) {
        KotlinRefactoringSettings.instance.renameOverloads = enabled
    }

    override fun createRenamer(element: PsiElement, newName: String, usages: Collection<UsageInfo>) =
        AutomaticOverloadsRenamer(element as KtNamedFunction, newName)
}