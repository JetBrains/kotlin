// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.util

import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil.lastChild
import org.jetbrains.annotations.ApiStatus
import kotlin.reflect.KClass

inline fun <reified T : PsiElement> PsiElement.parentOfType(): T? = parentOfType(T::class)

fun <T : PsiElement> PsiElement.parentOfType(vararg classes: KClass<out T>): T? {
  return PsiTreeUtil.getParentOfType(this, *classes.map { it.java }.toTypedArray())
}


inline fun <reified T : PsiElement> PsiElement.parentsOfType(): Sequence<T> = parentsOfType(T::class.java)

fun <T : PsiElement> PsiElement.parentsOfType(clazz: Class<out T>): Sequence<T> = parents().filterIsInstance(clazz)

fun PsiElement.parents(): Sequence<PsiElement> = generateSequence(this) { it.parent }

fun PsiElement.strictParents(): Sequence<PsiElement> = parents().drop(1)

private typealias ElementAndOffset = Pair<PsiElement, Int>

@ApiStatus.Experimental
fun PsiFile.elementsAroundOffsetUp(offset: Int): Iterable<ElementAndOffset> = Iterable {
  iterator<ElementAndOffset> {
    elementsAroundOffsetUp(this@elementsAroundOffsetUp, offset)
  }
}

private suspend fun SequenceScope<ElementAndOffset>.elementsAroundOffsetUp(root: PsiFile, offset: Int) {
  val leaf = root.findElementAt(offset) ?: return
  val offsetInLeaf = offset - leaf.textRange.startOffset
  if (offsetInLeaf == 0) {
    elementsAroundOffsetUp(leaf)
  }
  else {
    elementsAtOffsetUp(leaf, offsetInLeaf)
  }
}

private suspend fun SequenceScope<ElementAndOffset>.elementsAroundOffsetUp(leaf: PsiElement) {
  // Example: foo.bar<caret>[42]
  // Leaf is `[` element.
  // If we'd only process `[` and up, then we'd miss `bar` element (and its references/declarations).
  // ----------------------------------------
  //                 `foo.bar[42]`
  //                /             \
  //       `foo.bar`               `[42]`
  //      /    |    \             /  |   \
  // `foo`    `.`    `bar`     `[`  `42`  `]`
  // ----------------------------------------
  // We want to still give preference to the elements to the right of the caret,
  // since there may be references/declarations too.
  val leftSubTree = walkUpToCommonParent(leaf) ?: return
  // At this point elements to the right of the caret (`[` and `[42]`) are processed.
  // The sibling on the left (`foo.bar`) might be a tree,
  // so we should go up from the rightmost leaf of that tree (`bar`).
  val rightMostChild = lastChild(leftSubTree)
  // Since the subtree to the right of the caret was already processed,
  // we don't stop at common parent and just go up to the top.
  elementsAtOffsetUp(rightMostChild, rightMostChild.textLength)
}

private suspend fun SequenceScope<ElementAndOffset>.walkUpToCommonParent(leaf: PsiElement): PsiElement? {
  var current = leaf
  while (true) {
    ProgressManager.checkCanceled()
    yield(ElementAndOffset(current, 0))
    if (current is PsiFile) {
      return null
    }
    current.prevSibling?.let {
      return it
    }
    current = current.parent ?: return null
  }
}

private suspend fun SequenceScope<ElementAndOffset>.elementsAtOffsetUp(element: PsiElement, offsetInElement: Int) {
  var currentElement = element
  var currentOffset = offsetInElement
  while (true) {
    ProgressManager.checkCanceled()
    yield(ElementAndOffset(currentElement, currentOffset))
    if (currentElement is PsiFile) {
      return
    }
    currentOffset += currentElement.startOffsetInParent
    currentElement = currentElement.parent ?: return
  }
}

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
