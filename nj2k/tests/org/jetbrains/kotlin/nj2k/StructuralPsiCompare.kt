/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.psi.KtFile

internal fun KtFile.dumpStructureText(): String {
    val sb = StringBuilder()
    this.accept(
        object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element is PsiWhiteSpace) {
                    if (sb.lastOrNull() !in listOf(' ', '{', '}', '(', ')')) {
                        sb.append(" ")
                    }
                    return
                }
                if (element is LeafPsiElement) {
                    sb.append(element.text)
                    return
                }
                element.acceptChildren(this)
            }
        },
    )

    return sb.toString().trim()
}