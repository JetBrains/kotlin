/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.move

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.refactoring.move.MoveHandlerDelegate

// FIX ME WHEN BUNCH 191 REMOVED
internal fun MoveHandlerDelegate.canMoveCompat(
    elements: Array<out PsiElement>,
    targetContainer: PsiElement?,
    reference: PsiReference?
): Boolean = canMove(elements, targetContainer, reference)
