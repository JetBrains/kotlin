/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

val DataContext.project: Project
    get() = CommonDataKeys.PROJECT.getData(this)!!

val DataContext.hostEditor: Editor?
    get() = CommonDataKeys.HOST_EDITOR.getData(this)

val DataContext.psiElement: PsiElement?
    get() = CommonDataKeys.PSI_ELEMENT.getData(this)