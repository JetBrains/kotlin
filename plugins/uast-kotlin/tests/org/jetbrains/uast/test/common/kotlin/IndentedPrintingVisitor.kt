/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.uast.test.common.kotlin

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import org.jetbrains.uast.UFile
import kotlin.reflect.KClass

abstract class IndentedPrintingVisitor(val shouldIndent: (PsiElement) -> Boolean) : PsiElementVisitor() {

    constructor(vararg kClasses: KClass<*>) : this({ psi -> kClasses.any { it.isInstance(psi) } })

    private val builder = StringBuilder()
    var level = 0
        private set

    override fun visitElement(element: PsiElement) {
        val charSequence = render(element)
        if (charSequence != null) {
            builder.append("    ".repeat(level))
            builder.append(charSequence)
            builder.appendln()
        }

        val shouldIndent = shouldIndent(element)
        if (shouldIndent) level++
        element.acceptChildren(this)
        if (shouldIndent) level--
    }

    protected abstract fun render(element: PsiElement): CharSequence?

    val result: String
        get() = builder.toString()
}

fun IndentedPrintingVisitor.visitUFileAndGetResult(uFile: UFile): String {
    (uFile.sourcePsi as PsiFile).accept(this)
    return result
}