/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.rename

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.rename.PsiElementRenameHandler
import org.jetbrains.kotlin.asJava.findFacadeClass
import org.jetbrains.kotlin.asJava.toLightMethods
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isPlain
import org.jetbrains.kotlin.psi.psiUtil.plainContent
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class RenameJvmNameHandler : PsiElementRenameHandler() {
    private fun getStringTemplate(dataContext: DataContext): KtStringTemplateExpression? {
        val caret = CommonDataKeys.CARET.getData(dataContext) ?: return null
        val ktFile = CommonDataKeys.PSI_FILE.getData(dataContext) as? KtFile ?: return null
        return ktFile.findElementAt(caret.offset)?.getNonStrictParentOfType<KtStringTemplateExpression>()
    }

    override fun isAvailableOnDataContext(dataContext: DataContext): Boolean {
        val nameExpression = getStringTemplate(dataContext) ?: return false
        if (!nameExpression.isPlain()) return false
        val entry = ((nameExpression.parent as? KtValueArgument)?.parent as? KtValueArgumentList)?.parent as? KtAnnotationEntry
            ?: return false
        val annotationType = entry.analyze(BodyResolveMode.PARTIAL)[BindingContext.TYPE, entry.typeReference]
            ?: return false
        return annotationType.constructor.declarationDescriptor?.importableFqName == DescriptorUtils.JVM_NAME
    }

    private fun wrapDataContext(dataContext: DataContext): DataContext? {
        val nameExpression = getStringTemplate(dataContext) ?: return null
        val name = nameExpression.plainContent
        val entry = nameExpression.getStrictParentOfType<KtAnnotationEntry>() ?: return null
        val newElement =
            when (val annotationList = PsiTreeUtil.getParentOfType(entry, KtModifierList::class.java, KtFileAnnotationList::class.java)) {
                is KtModifierList -> (annotationList.parent as? KtDeclaration)?.toLightMethods()?.firstOrNull { it.name == name }
                    ?: return null

                is KtFileAnnotationList -> annotationList.getContainingKtFile().findFacadeClass() ?: return null

                else -> return null
            }
        return DataContext { id ->
            if (CommonDataKeys.PSI_ELEMENT.`is`(id)) return@DataContext newElement
            dataContext.getData(id)
        }
    }

    override fun invoke(project: Project, editor: Editor, file: PsiFile, dataContext: DataContext) {
        super.invoke(project, editor, file, wrapDataContext(dataContext) ?: return)
    }

    override fun invoke(project: Project, elements: Array<out PsiElement>, dataContext: DataContext) {
        super.invoke(project, elements, wrapDataContext(dataContext) ?: return)
    }
}
