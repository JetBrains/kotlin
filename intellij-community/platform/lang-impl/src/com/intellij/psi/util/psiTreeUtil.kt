// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.util

import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import kotlin.reflect.KClass

inline fun <reified T : PsiElement> PsiElement.parentOfType(): T? = parentOfType(T::class)

fun <T : PsiElement> PsiElement.parentOfType(vararg classes: KClass<out T>): T? {
  return PsiTreeUtil.getParentOfType(this, *classes.map { it.java }.toTypedArray())
}


inline fun <reified T : PsiElement> PsiElement.parentsOfType(): Sequence<T> = parentsOfType(T::class.java)

fun <T : PsiElement> PsiElement.parentsOfType(clazz: Class<out T>): Sequence<T> = parents().filterIsInstance(clazz)

fun PsiElement.parents(): Sequence<PsiElement> = generateSequence(this) { it.parent }

fun PsiElement.strictParents(): Sequence<PsiElement> = parents().drop(1)

inline fun <reified T : PsiElement> PsiElement.contextOfType(): T? = contextOfType(T::class)

fun <T : PsiElement> PsiElement.contextOfType(vararg classes: KClass<out T>): T? {
  return PsiTreeUtil.getContextOfType(this, *classes.map { it.java }.toTypedArray())
}

fun PsiElement.siblings(forward: Boolean = true): Sequence<PsiElement> {
  return generateSequence(this) {
    if (forward) {
      it.nextSibling
    }
    else {
      it.prevSibling
    }
  }
}

fun <T : PsiElement> Sequence<T>.skipTokens(tokens: TokenSet): Sequence<T> {
  return filter { it.node.elementType !in tokens }
}
