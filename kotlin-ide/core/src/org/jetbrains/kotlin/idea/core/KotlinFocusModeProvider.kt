/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core

import com.intellij.codeInsight.daemon.impl.focusMode.FocusModeProvider
import com.intellij.openapi.util.Segment
import com.intellij.psi.PsiAnonymousClass
import com.intellij.psi.PsiFile
import com.intellij.psi.SyntaxTraverser
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClass

class KotlinFocusModeProvider : FocusModeProvider {
    override fun calcFocusZones(file: PsiFile): MutableList<out Segment> =
        SyntaxTraverser.psiTraverser(file)
            .postOrderDfsTraversal()
            .filter {
                it is KtClassOrObject || it is KtFunction || it is KtClassInitializer
            }
            .filter {
                val p = it.parent
                p is KtFile || p is KtClassBody
            }
            .map {
                it.textRange
            }.toMutableList()
}