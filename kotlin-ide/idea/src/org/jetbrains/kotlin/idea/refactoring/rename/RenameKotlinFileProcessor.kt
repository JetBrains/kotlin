/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.rename

import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.refactoring.listeners.RefactoringElementListener
import com.intellij.refactoring.rename.RenamePsiFileProcessor
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.psi.KtFile
import java.util.ArrayList

class RenameKotlinFileProcessor : RenamePsiFileProcessor() {
    class FileRenamingPsiClassWrapper(
        private val psiClass: KtLightClass,
        private val file: KtFile
    ) : KtLightClass by psiClass {
        override fun isValid() = file.isValid
    }

    override fun canProcessElement(element: PsiElement) =
        element is KtFile && ProjectRootsUtil.isInProjectSource(element, includeScriptsOutsideSourceRoots = true)

    override fun prepareRenaming(
        element: PsiElement,
        newName: String,
        allRenames: MutableMap<PsiElement, String>,
        scope: SearchScope
    ) {
        val jetFile = element as? KtFile ?: return
        if (FileTypeManager.getInstance().getFileTypeByFileName(newName) != KotlinFileType.INSTANCE) {
            return
        }

        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return

        val fileInfo = JvmFileClassUtil.getFileClassInfoNoResolve(jetFile)
        if (!fileInfo.withJvmName) {
            val facadeFqName = fileInfo.facadeClassFqName
            val project = jetFile.project
            val facadeClass = JavaPsiFacade.getInstance(project)
                .findClass(facadeFqName.asString(), GlobalSearchScope.moduleScope(module)) as? KtLightClass
            if (facadeClass != null) {
                allRenames[FileRenamingPsiClassWrapper(facadeClass, jetFile)] = PackagePartClassUtils.getFilePartShortName(newName)
            }
        }
    }

    override fun renameElement(element: PsiElement, newName: String, usages: Array<UsageInfo>, listener: RefactoringElementListener?) {
        val kotlinUsages = ArrayList<UsageInfo>(usages.size)

        ForeignUsagesRenameProcessor.processAll(element, newName, usages, fallbackHandler = {
            kotlinUsages += it
        })

        super.renameElement(element, newName, kotlinUsages.toTypedArray(), listener)
    }
}
