package com.jetbrains.kotlin.structuralsearch.impl.matcher

import com.intellij.psi.PsiErrorElement
import com.intellij.structuralsearch.MalformedPatternException

class KotlinValidationVisitor : KotlinRecursiveElementVisitor() {
    override fun visitErrorElement(element: PsiErrorElement) {
        super.visitErrorElement(element)
        val errorDescription = element.errorDescription
        throw MalformedPatternException(errorDescription)
    }
}