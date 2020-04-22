package com.jetbrains.kotlin.structuralsearch

import com.intellij.codeInsight.template.TemplateContextType
import com.intellij.dupLocator.util.NodeFilter
import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.DebugUtil
import com.intellij.structuralsearch.StructuralSearchProfile
import com.intellij.structuralsearch.impl.matcher.CompiledPattern
import com.intellij.structuralsearch.impl.matcher.GlobalMatchingVisitor
import com.intellij.structuralsearch.impl.matcher.PatternTreeContext
import com.intellij.structuralsearch.impl.matcher.compiler.GlobalCompilingVisitor
import com.intellij.util.SmartList
import com.jetbrains.kotlin.structuralsearch.impl.matcher.KotlinCompiledPattern
import com.jetbrains.kotlin.structuralsearch.impl.matcher.KotlinMatchingVisitor
import com.jetbrains.kotlin.structuralsearch.impl.matcher.compiler.KotlinCompilingVisitor
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.liveTemplates.KotlinTemplateContextType
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPsiFactory

class KotlinStructuralSearchProfile : StructuralSearchProfile() {
    override fun getLexicalNodesFilter(): NodeFilter {
        return NodeFilter { element -> element is PsiWhiteSpace }
    }

    override fun createMatchingVisitor(globalVisitor: GlobalMatchingVisitor): PsiElementVisitor {
        return KotlinMatchingVisitor(globalVisitor)
    }

    override fun createCompiledPattern(): CompiledPattern {
        return KotlinCompiledPattern()
    }

    override fun compile(elements: Array<out PsiElement>?, globalVisitor: GlobalCompilingVisitor) {
        KotlinCompilingVisitor(globalVisitor).compile(elements)
    }

    override fun isMyLanguage(language: Language): Boolean {
        return language == KotlinLanguage.INSTANCE
    }

    override fun getTemplateContextTypeClass(): Class<out TemplateContextType> {
        return KotlinTemplateContextType::class.java
    }

    override fun createPatternTree(
        text: String,
        context: PatternTreeContext,
        fileType: LanguageFileType,
        language: Language,
        contextId: String?,
        project: Project,
        physical: Boolean
    ): Array<PsiElement> {
        fun retryWith(ctx: PatternTreeContext) =
            createPatternTree(text, ctx, fileType, language, contextId, project, physical)
        when (context) {
            PatternTreeContext.Block -> {
                val fragment = KtPsiFactory(project).createBlockCodeFragment(text, null)
                val result = getNonWhitespaceChildren(fragment)
                if (result.isEmpty()) return PsiElement.EMPTY_ARRAY

                return when {
                    shouldTryExpressionPattern(result) -> retryWith(PatternTreeContext.Expression)
                    else -> {
                        println(DebugUtil.psiToString(result.first().parent, false))
                        arrayOf(result.first())
                    }
                }
            }
            PatternTreeContext.Expression -> {
                val fragment = KtPsiFactory(project).createExpressionCodeFragment(text, null)
                val content = fragment.getContentElement() ?: return PsiElement.EMPTY_ARRAY
                if (content != PsiElement.EMPTY_ARRAY) println(DebugUtil.psiToString(content.parent, false))
                return arrayOf(content)
            }
            else -> {
                val psiFile =
                    PsiFileFactory.getInstance(project).createFileFromText("__dummy.kt", KotlinFileType.INSTANCE, text)
                if (psiFile != PsiElement.EMPTY_ARRAY) println(DebugUtil.psiToString(psiFile, false))
                return arrayOf(psiFile)
            }
        }
    }

    companion object {

        fun getNonWhitespaceChildren(fragment: PsiElement): List<PsiElement> {
            var element = fragment.firstChild
            val result: MutableList<PsiElement> =
                SmartList()
            while (element != null) {
                if (element !is PsiWhiteSpace) {
                    result.add(element)
                }
                element = element.nextSibling
            }
            return result
        }

        private fun shouldTryExpressionPattern(elements: List<PsiElement>): Boolean {
            val element = elements.first().firstChild
            return elements.first().children.size == 1 && element is KtExpression && element !is KtClass
        }

    }

}