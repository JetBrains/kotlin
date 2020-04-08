/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.util

import com.intellij.openapi.module.Module
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiJavaModule
import com.intellij.psi.PsiRequiresStatement
import com.intellij.psi.search.FilenameIndex

fun findFirstPsiJavaModule(module: Module): PsiJavaModule? {
    val project = module.project

    val moduleInfoFiles = FilenameIndex.getFilesByName(project, PsiJavaModule.MODULE_INFO_FILE, module.moduleScope)
    return moduleInfoFiles.asSequence().filterIsInstance<PsiJavaFile>().map { it.moduleDeclaration }.firstOrNull { it != null }
}

fun findRequireDirective(module: PsiJavaModule, requiredName: String): PsiRequiresStatement? =
    module.requires.find { it.moduleName == requiredName }
