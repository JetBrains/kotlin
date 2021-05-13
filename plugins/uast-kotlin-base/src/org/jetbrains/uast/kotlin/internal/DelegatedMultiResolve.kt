/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin.internal

import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.ResolveResult
import com.intellij.psi.infos.CandidateInfo
import org.jetbrains.uast.UMultiResolvable
import org.jetbrains.uast.UResolvable

interface DelegatedMultiResolve : UMultiResolvable, UResolvable {
    override fun multiResolve(): Iterable<ResolveResult> = listOfNotNull(resolve()?.let { CandidateInfo(it, PsiSubstitutor.EMPTY) })
}
