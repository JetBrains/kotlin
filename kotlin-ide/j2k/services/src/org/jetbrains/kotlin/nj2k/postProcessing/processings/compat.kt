/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.postProcessing.processings

import com.intellij.psi.PsiReference
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import org.jetbrains.kotlin.idea.search.or
import org.jetbrains.kotlin.idea.search.projectScope
import org.jetbrains.kotlin.psi.KtParameter

// FIX ME WHEN BUNCH 191 REMOVED

internal fun KtParameter.findReferences(renamer: RenamePsiElementProcessor): MutableCollection<PsiReference> {
    val searchScope = this.project.projectScope() or this.useScope
    return renamer.findReferences(this, searchScope, false)
}
