// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.util

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.DebugUtil

val LOG = Logger.getInstance("org.jetbrains.plugins.gradle.util.PsiTestUtil")

fun <R> runReadActionAndWait(action: () -> R): R {
  val application = ApplicationManager.getApplication()
  val result = Ref<R>()
  application.invokeAndWait {
    application.runReadAction {
      result.set(action())
    }
  }
  return result.get()
}

fun PsiElement.findChildByElementType(typeName: String) =
  findChildrenByElementType(typeName).first()

fun PsiElement.findChildrenByElementType(typeName: String): List<PsiElement> {
  val elements = children.filter { it.node.elementType.toString() == typeName }
  if (elements.isNotEmpty()) return elements
  printPsiStructure()
  LOG.serve("PsiElement[$typeName] not found in $this")
}

inline fun <reified T : PsiElement> PsiElement.findChildByType(): T =
  findChildrenByType<T>().first()

inline fun <reified T : PsiElement> PsiElement.findChildrenByType(): List<T> {
  val elements = children.filterIsInstance<T>()
  if (elements.isNotEmpty()) return elements
  LOG.warn("\n" + DebugUtil.psiToString(this, true))
  printPsiStructure()
  LOG.serve("${T::class.java} not found in $this")
}

fun PsiElement.printPsiStructure(indent: Int = 0) {
  val prefix = "  ".repeat(indent)
  LOG.warn(prefix + node.elementType.toString())
  children.forEach { it.printPsiStructure(indent + 1) }
}

@Suppress("CAST_NEVER_SUCCEEDS")
fun Logger.serve(message: String): Nothing =
  error(message) as Nothing
