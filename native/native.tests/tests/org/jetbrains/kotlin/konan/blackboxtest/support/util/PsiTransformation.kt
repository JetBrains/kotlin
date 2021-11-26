/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support.util

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.nextLeaf
import org.jetbrains.kotlin.psi.psiUtil.prevLeaf

internal fun PsiElement.ensureSurroundedByWhiteSpace(): PsiElement =
    ensureHasWhiteSpaceBefore().ensureHasWhiteSpaceAfter()

private fun PsiElement.ensureHasWhiteSpaceBefore(): PsiElement {
    val (fileBoundaryReached, whiteSpaceBefore) = whiteSpaceBefore()
    if (!fileBoundaryReached and !whiteSpaceBefore.endsWith(" ")) {
        parent.addBefore(KtPsiFactory(project).createWhiteSpace(" "), this)
    }
    return this
}

private fun PsiElement.ensureHasWhiteSpaceAfter(): PsiElement {
    val (fileBoundaryReached, whiteSpaceAfter) = whiteSpaceAfter()
    if (!fileBoundaryReached and !whiteSpaceAfter.startsWith(" ")) {
        parent.addAfter(KtPsiFactory(project).createWhiteSpace(" "), this)
    }
    return this
}

private fun PsiElement.whiteSpaceBefore(): Pair<Boolean, String> {
    var fileBoundaryReached = false

    fun PsiElement.prevWhiteSpace(): PsiWhiteSpace? = when (val prevLeaf = prevLeaf(skipEmptyElements = true)) {
        null -> {
            fileBoundaryReached = true
            null
        }
        else -> prevLeaf as? PsiWhiteSpace
    }

    val whiteSpace = buildString {
        generateSequence(prevWhiteSpace()) { it.prevWhiteSpace() }.toList().asReversed().forEach { append(it.text) }
    }

    return fileBoundaryReached to whiteSpace
}

private fun PsiElement.whiteSpaceAfter(): Pair<Boolean, String> {
    var fileBoundaryReached = false

    fun PsiElement.nextWhiteSpace(): PsiWhiteSpace? = when (val nextLeaf = nextLeaf(skipEmptyElements = true)) {
        null -> {
            fileBoundaryReached = true
            null
        }
        else -> nextLeaf as? PsiWhiteSpace
    }

    val whiteSpace = buildString {
        generateSequence(nextWhiteSpace()) { it.nextWhiteSpace() }.forEach { append(it.text) }
    }

    return fileBoundaryReached to whiteSpace
}
