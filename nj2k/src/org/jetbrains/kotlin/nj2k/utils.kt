/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.parentsOfType

fun <T> List<T>.replace(element: T, replacer: T): List<T> {
    val mutableList = toMutableList()
    val index = indexOf(element)
    mutableList[index] = replacer
    return mutableList
}

inline fun <reified T : PsiElement> PsiElement.parentOfType(): T? =
    PsiTreeUtil.getParentOfType(this, T::class.java)

inline fun <reified T : PsiElement> PsiElement.parentsOfType(): Sequence<T> = parentsOfType(T::class.java)

