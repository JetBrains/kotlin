/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.nodejs

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.util.projectStructure.getModuleDir
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf

data class TestElementInfo<out Settings>(val runSettings: Settings, val enclosingTestElement: PsiElement)

sealed class TestElementPath {
    data class BySuite(val suiteNames: List<String>, val testName: String?) : TestElementPath()
    object BySingleFile : TestElementPath()

    companion object {
        fun isModuleAssociatedDir(element: PsiElement, module: Module): Boolean {
            if (element !is PsiDirectory) return false
            val virtualFile = element.virtualFile
            return (module.getModuleDir() == virtualFile.path
                    || virtualFile == ModuleRootManager.getInstance(module).contentRoots.singleOrNull())
        }

        fun forElement(element: PsiElement, module: Module): TestElementPath? {
            if (isModuleAssociatedDir(element, module)) return TestElementPath.BySingleFile

            val declaration = element.getNonStrictParentOfType<KtNamedDeclaration>() ?: return null
            val klass = when (declaration) {
                is KtClassOrObject -> declaration
                is KtNamedFunction -> declaration.containingClassOrObject ?: return null
                else -> return null
            }
            val suiteNames = klass.parentsWithSelf
                .filterIsInstance<KtClassOrObject>()
                .mapNotNull { it.name }
                .toList()
                .asReversed()
            val testName = (declaration as? KtNamedFunction)?.name
            return TestElementPath.BySuite(suiteNames, testName)
        }
    }
}