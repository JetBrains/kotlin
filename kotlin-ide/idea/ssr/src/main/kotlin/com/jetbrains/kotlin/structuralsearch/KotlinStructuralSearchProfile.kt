package com.jetbrains.kotlin.structuralsearch

import com.intellij.codeInsight.template.TemplateContextType
import com.intellij.codeInsight.template.XmlContextType
import com.intellij.dupLocator.util.NodeFilter
import com.intellij.lang.Language
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.structuralsearch.StructuralSearchProfile
import com.intellij.structuralsearch.impl.matcher.CompiledPattern
import com.intellij.structuralsearch.impl.matcher.GlobalMatchingVisitor
import com.intellij.structuralsearch.impl.matcher.XmlCompiledPattern
import com.intellij.structuralsearch.impl.matcher.XmlMatchingVisitor
import com.intellij.structuralsearch.impl.matcher.compiler.GlobalCompilingVisitor
import com.intellij.structuralsearch.impl.matcher.compiler.XmlCompilingVisitor
import org.jetbrains.kotlin.idea.KotlinLanguage

class KotlinStructuralSearchProfile : StructuralSearchProfile() {
    override fun getLexicalNodesFilter(): NodeFilter {
        return NodeFilter { element -> element is PsiWhiteSpace || element is PsiErrorElement }
    }

    override fun createMatchingVisitor(globalVisitor: GlobalMatchingVisitor): PsiElementVisitor {
        return XmlMatchingVisitor(globalVisitor)
    }

    override fun createCompiledPattern(): CompiledPattern {
        return XmlCompiledPattern()
    }

    override fun compile(elements: Array<out PsiElement>?, globalVisitor: GlobalCompilingVisitor) {
        XmlCompilingVisitor(globalVisitor).compile(elements)
    }

    override fun isMyLanguage(language: Language): Boolean {
        return language == KotlinLanguage.INSTANCE
    }

    override fun getTemplateContextTypeClass(): Class<out TemplateContextType> {
        return XmlContextType::class.java
    }

}