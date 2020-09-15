/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.search.usagesSearch

import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.*
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.MethodReferencesSearch
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.idea.asJava.LightClassProvider.Companion.providedToLightMethods
import org.jetbrains.kotlin.idea.search.KotlinSearchUsagesSupport
import org.jetbrains.kotlin.idea.search.KotlinSearchUsagesSupport.Companion.createConstructorHandle
import org.jetbrains.kotlin.idea.search.declarationsSearch.HierarchySearchRequest
import org.jetbrains.kotlin.idea.search.declarationsSearch.searchInheritors
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.contains

fun PsiElement.processDelegationCallConstructorUsages(scope: SearchScope, process: (KtCallElement) -> Boolean): Boolean {
    val task = buildProcessDelegationCallConstructorUsagesTask(scope, process)
    return task()
}

// should be executed under read-action, returns long-running part to be executed outside read-action
fun PsiElement.buildProcessDelegationCallConstructorUsagesTask(scope: SearchScope, process: (KtCallElement) -> Boolean): () -> Boolean {
    ApplicationManager.getApplication().assertReadAccessAllowed()
    val task1 = buildProcessDelegationCallKotlinConstructorUsagesTask(scope, process)
    val task2 = buildProcessDelegationCallJavaConstructorUsagesTask(scope, process)
    return { task1() && task2() }
}

private fun PsiElement.buildProcessDelegationCallKotlinConstructorUsagesTask(
    scope: SearchScope,
    process: (KtCallElement) -> Boolean
): () -> Boolean {
    val element = unwrapped
    if (element != null && element !in scope) return { true }

    val klass = when (element) {
        is KtConstructor<*> -> element.getContainingClassOrObject()
        is KtClass -> element
        else -> return { true }
    }

    if (klass !is KtClass || element !is KtDeclaration) return { true }

    val constructorHandler = createConstructorHandle(element)

    if (!processClassDelegationCallsToSpecifiedConstructor(klass, constructorHandler, process)) return { false }

    // long-running task, return it to execute outside read-action
    return { processInheritorsDelegatingCallToSpecifiedConstructor(klass, scope, constructorHandler, process) }
}

private fun PsiElement.buildProcessDelegationCallJavaConstructorUsagesTask(
    scope: SearchScope,
    process: (KtCallElement) -> Boolean
): () -> Boolean {
    if (this is KtLightElement<*, *>) return { true }
    // TODO: Temporary hack to avoid NPE while KotlinNoOriginLightMethod is around
    if (this is KtLightMethod && this.kotlinOrigin == null) return { true }
    if (!(this is PsiMethod && isConstructor)) return { true }
    val klass = containingClass ?: return { true }

    val ctorHandle = createConstructorHandle(this)
    return { processInheritorsDelegatingCallToSpecifiedConstructor(klass, scope, ctorHandle, process) }
}


private fun processInheritorsDelegatingCallToSpecifiedConstructor(
    klass: PsiElement,
    scope: SearchScope,
    constructorCallComparator: KotlinSearchUsagesSupport.ConstructorCallHandle,
    process: (KtCallElement) -> Boolean
): Boolean {
    return HierarchySearchRequest(klass, scope, false).searchInheritors().all {
        runReadAction {
            val unwrapped = it.takeIf { it.isValid }?.unwrapped
            if (unwrapped is KtClass)
                processClassDelegationCallsToSpecifiedConstructor(unwrapped, constructorCallComparator, process)
            else
                true
        }
    }
}

private fun processClassDelegationCallsToSpecifiedConstructor(
    klass: KtClass,
    constructorCallHadle: KotlinSearchUsagesSupport.ConstructorCallHandle,
    process: (KtCallElement) -> Boolean
): Boolean {
    for (secondaryConstructor in klass.secondaryConstructors) {
        val delegationCall = secondaryConstructor.getDelegationCall()
        if (constructorCallHadle.referencedTo(delegationCall)) {
            if (!process(delegationCall)) return false
        }
    }
    if (!klass.isEnum()) return true
    for (declaration in klass.declarations) {
        if (declaration is KtEnumEntry) {
            val delegationCall =
                declaration.superTypeListEntries.firstOrNull() as? KtSuperTypeCallEntry
                    ?: continue

            if (constructorCallHadle.referencedTo(delegationCall.calleeExpression)) {
                if (!process(delegationCall)) return false
            }
        }
    }
    return true
}

fun PsiElement.searchReferencesOrMethodReferences(): Collection<PsiReference> {
    val lightMethods = providedToLightMethods()
    return if (lightMethods.isNotEmpty()) {
        lightMethods.flatMapTo(LinkedHashSet()) { MethodReferencesSearch.search(it) }
    } else {
        ReferencesSearch.search(this).findAll()
    }
}