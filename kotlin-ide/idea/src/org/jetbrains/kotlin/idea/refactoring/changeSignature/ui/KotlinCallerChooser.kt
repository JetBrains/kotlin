/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.changeSignature.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReference
import com.intellij.psi.search.searches.MethodReferencesSearch
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.refactoring.changeSignature.CallerChooserBase
import com.intellij.refactoring.changeSignature.MemberNodeBase
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.Consumer
import com.intellij.util.ui.UIUtil
import org.jetbrains.kotlin.asJava.getRepresentativeLightMethod
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.jetbrains.kotlin.asJava.toLightMethods
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.util.getJavaMethodDescriptor
import org.jetbrains.kotlin.idea.hierarchy.calls.CalleeReferenceProcessor
import org.jetbrains.kotlin.idea.hierarchy.calls.KotlinCallHierarchyNodeDescriptor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import java.util.*

class KotlinCallerChooser(
    declaration: PsiElement,
    project: Project,
    @NlsContexts.DialogTitle title: String,
    previousTree: Tree?,
    callback: Consumer<Set<PsiElement>>
) : CallerChooserBase<PsiElement>(declaration, project, title, previousTree, "dummy." + KotlinFileType.EXTENSION, callback) {
    override fun createTreeNodeFor(method: PsiElement?, called: HashSet<PsiElement>?, cancelCallback: Runnable?) =
        KotlinMethodNode(method, called ?: HashSet(), myProject, cancelCallback ?: Runnable {})

    override fun findDeepestSuperMethods(method: PsiElement) = method.toLightMethods().singleOrNull()?.findDeepestSuperMethods()

    override fun getEmptyCallerText() = KotlinBundle.message("text.caller.text.with.highlighted.callee.call.would.be.shown.here")

    override fun getEmptyCalleeText() = KotlinBundle.message("text.callee.text.would.be.shown.here")
}

class KotlinMethodNode(
    method: PsiElement?,
    called: HashSet<PsiElement>,
    project: Project,
    cancelCallback: Runnable
) : MemberNodeBase<PsiElement>(method?.namedUnwrappedElement ?: method, called, project, cancelCallback) {
    override fun createNode(caller: PsiElement, called: HashSet<PsiElement>) = KotlinMethodNode(caller, called, myProject, myCancelCallback)

    override fun customizeRendererText(renderer: ColoredTreeCellRenderer) {
        val descriptor = when (myMethod) {
            is KtFunction -> myMethod.unsafeResolveToDescriptor() as FunctionDescriptor
            is KtClass -> (myMethod.unsafeResolveToDescriptor() as ClassDescriptor).unsubstitutedPrimaryConstructor ?: return
            is PsiMethod -> myMethod.getJavaMethodDescriptor() ?: return
            else -> throw AssertionError("Invalid declaration: ${myMethod.getElementTextWithContext()}")
        }
        val containerName = generateSequence<DeclarationDescriptor>(descriptor) { it.containingDeclaration }
            .firstOrNull { it is ClassDescriptor }
            ?.name

        val renderedFunction = KotlinCallHierarchyNodeDescriptor.renderNamedFunction(descriptor)
        val renderedFunctionWithContainer = containerName?.let {
            "${if (it.isSpecial) KotlinBundle.message("text.anonymous") else it.asString()}.$renderedFunction"
        } ?: renderedFunction

        val attributes = if (isEnabled)
            SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, UIUtil.getTreeForeground())
        else
            SimpleTextAttributes.EXCLUDED_ATTRIBUTES
        renderer.append(renderedFunctionWithContainer, attributes)

        val packageName = (myMethod.containingFile as? PsiClassOwner)?.packageName ?: ""
        renderer.append("  ($packageName)", SimpleTextAttributes(SimpleTextAttributes.STYLE_ITALIC, JBColor.GRAY))
    }

    override fun computeCallers(): List<PsiElement> {
        if (myMethod == null) return emptyList()

        val callers = LinkedHashSet<PsiElement>()

        val processor = object : CalleeReferenceProcessor(false) {
            override fun onAccept(ref: PsiReference, element: PsiElement) {
                if ((element is KtFunction || element is KtClass || element is PsiMethod) && element !in myCalled) {
                    callers.add(element)
                }
            }
        }
        val query = myMethod.getRepresentativeLightMethod()?.let { MethodReferencesSearch.search(it, it.useScope, true) }
            ?: ReferencesSearch.search(myMethod, myMethod.useScope)
        query.forEach { processor.process(it) }
        return callers.toList()
    }
}