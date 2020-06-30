package com.jetbrains.kotlin.structuralsearch

import com.intellij.dupLocator.util.NodeFilter
import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.DebugUtil
import com.intellij.structuralsearch.*
import com.intellij.structuralsearch.impl.matcher.CompiledPattern
import com.intellij.structuralsearch.impl.matcher.GlobalMatchingVisitor
import com.intellij.structuralsearch.impl.matcher.PatternTreeContext
import com.intellij.structuralsearch.impl.matcher.compiler.GlobalCompilingVisitor
import com.intellij.structuralsearch.impl.matcher.predicates.MatchPredicate
import com.intellij.structuralsearch.impl.matcher.predicates.NotPredicate
import com.intellij.structuralsearch.plugin.replace.ReplaceOptions
import com.intellij.structuralsearch.plugin.ui.Configuration
import com.intellij.structuralsearch.plugin.ui.UIUtil
import com.intellij.util.SmartList
import com.jetbrains.kotlin.structuralsearch.visitor.KotlinCompilingVisitor
import com.jetbrains.kotlin.structuralsearch.visitor.KotlinMatchingVisitor
import com.jetbrains.kotlin.structuralsearch.visitor.KotlinRecursiveElementWalkingVisitor
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.liveTemplates.KotlinTemplateContextType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*

class KotlinStructuralSearchProfile : StructuralSearchProfile() {
    override fun getLexicalNodesFilter(): NodeFilter = NodeFilter { element -> element is PsiWhiteSpace }

    override fun createMatchingVisitor(globalVisitor: GlobalMatchingVisitor): KotlinMatchingVisitor =
        KotlinMatchingVisitor(globalVisitor)

    override fun createCompiledPattern(): CompiledPattern = object : CompiledPattern() {
        init {
            strategy = KotlinMatchingStrategy
        }

        override fun getTypedVarPrefixes(): Array<String> = arrayOf(TYPED_VAR_PREFIX)

        override fun isTypedVar(str: String): Boolean = when {
            str.isEmpty() -> false
            str[0] == '@' -> str.regionMatches(1, TYPED_VAR_PREFIX, 0, TYPED_VAR_PREFIX.length)
            else -> str.startsWith(TYPED_VAR_PREFIX)
        }
    }

    override fun isMyLanguage(language: Language): Boolean = language == KotlinLanguage.INSTANCE

    override fun getTemplateContextTypeClass(): Class<KotlinTemplateContextType> = KotlinTemplateContextType::class.java

    override fun getPredefinedTemplates(): Array<Configuration> = KotlinPredefinedConfigurations.createPredefinedTemplates()

    override fun getDefaultFileType(fileType: LanguageFileType?): LanguageFileType = fileType ?: KotlinFileType.INSTANCE

    override fun compile(elements: Array<out PsiElement>?, globalVisitor: GlobalCompilingVisitor) {
        KotlinCompilingVisitor(globalVisitor).compile(elements)
    }

    override fun getPresentableElement(element: PsiElement?): PsiElement {
        val pElement = super.getPresentableElement(element)
        val parent = pElement.parent
        return if (parent is KtProperty || parent is KtNamedFunction || parent is KtClass) parent else pElement
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
        var elements: List<PsiElement> = listOf()
        if (PROPERTY_CONTEXT.id == contextId) {
            val fragment = KtPsiFactory(project, false).createProperty(text)
            elements = listOf(getNonWhitespaceChildren(fragment).first().parent)
            if (elements.first() !is KtProperty) return PsiElement.EMPTY_ARRAY
        } else {
            val fragment = KtPsiFactory(project, false).createBlockCodeFragment("Unit\n$text", null)
            elements = when (fragment.lastChild) {
                is PsiComment -> getNonWhitespaceChildren(fragment).drop(1)
                else -> getNonWhitespaceChildren(fragment.firstChild).drop(1)
            }
        }

        if (elements.isEmpty()) return PsiElement.EMPTY_ARRAY

        // Standalone KtAnnotationEntry support
        if (elements.first() is KtAnnotatedExpression && elements.first().lastChild is PsiErrorElement)
            elements = getNonWhitespaceChildren(elements.first()).dropLast(1)

        for (element in elements) print(DebugUtil.psiToString(element, false))

        return elements.toTypedArray()
    }

    inner class KotlinValidator : KotlinRecursiveElementWalkingVisitor() {
        override fun visitErrorElement(element: PsiErrorElement) {
            super.visitErrorElement(element)
            if (shouldShowProblem(element)) {
                throw MalformedPatternException(element.errorDescription)
            }
        }
    }

    override fun checkSearchPattern(pattern: CompiledPattern) {
        val visitor = KotlinValidator()
        val nodes = pattern.nodes
        while (nodes.hasNext()) {
            nodes.current().accept(visitor)
            nodes.advance()
        }
        nodes.reset()
    }

    override fun shouldShowProblem(error: PsiErrorElement): Boolean {
        val description = error.errorDescription
        val parent = error.parent
        return when {
            parent is KtTryExpression && KSSRBundle.message("error.expected.catch.or.finally") == description -> false //naked try
            parent is KtAnnotatedExpression && KSSRBundle.message("error.expected.an.expression") == description -> false
            else -> true
        }
    }

    override fun checkReplacementPattern(project: Project, options: ReplaceOptions) {
        val matchOptions = options.matchOptions
        val fileType = matchOptions.fileType
        val dialect = matchOptions.dialect
        val searchIsDeclaration = isProbableExpression(matchOptions.searchPattern, fileType, dialect, project)
        val replacementIsDeclaration = isProbableExpression(options.replacement, fileType, dialect, project)
        if (searchIsDeclaration != replacementIsDeclaration) {
            throw UnsupportedPatternException(
                if (searchIsDeclaration) SSRBundle.message("replacement.template.is.not.expression.error.message")
                else SSRBundle.message("search.template.is.not.expression.error.message")
            )
        }
    }

    override fun isIdentifier(element: PsiElement?): Boolean = element != null && element.node.elementType == KtTokens.IDENTIFIER

    override fun isApplicableConstraint(
        constraintName: String?,
        variableNode: PsiElement?,
        completePattern: Boolean,
        target: Boolean
    ): Boolean {
        when (constraintName) {
            UIUtil.TYPE -> variableNode?.let { varNode ->
                val parent: PsiElement? = varNode.parent
                val gParent: PsiElement? = parent?.parent
                val ggParent: PsiElement? = gParent?.parent
                val gggParent: PsiElement? = ggParent?.parent
                return@isApplicableConstraint when {
                    gParent is KtDeclarationWithInitializer && gParent.initializer == parent -> false
                    gggParent is KtCallableDeclaration && gggParent.typeReference == ggParent -> false
                    parent is KtExpression -> true
                    else -> false
                }
            } ?: return false
        }
        return super.isApplicableConstraint(constraintName, variableNode, completePattern, target)
    }

    override fun getCustomPredicates(
        constraint: MatchVariableConstraint?,
        name: String,
        options: MatchOptions
    ): MutableList<MatchPredicate> {
        val result = SmartList<MatchPredicate>()
        if (!StringUtil.isEmptyOrSpaces(constraint!!.expressionTypes)) {
            val predicate = KotlinExprTypePredicate(
                searchedTypeNames = constraint.expressionTypes.split("|"),
                withinHierachy = constraint.isExprTypeWithinHierarchy,
                ignoreCase = !options.isCaseSensitiveMatch
            )
            result.add(if (constraint.isInvertExprType) NotPredicate(predicate) else predicate)
        }
        return result
    }

    private fun isProbableExpression(pattern: String, fileType: LanguageFileType, dialect: Language, project: Project): Boolean {
        val searchElements = createPatternTree(pattern, PatternTreeContext.Block, fileType, dialect, null, project, false)
        return searchElements[0] is KtDeclaration
    }

    override fun getReplaceHandler(project: Project, replaceOptions: ReplaceOptions): DocumentBasedReplaceHandler =
        DocumentBasedReplaceHandler(project)

    override fun getPatternContexts(): MutableList<PatternContext> = PATTERN_CONTEXTS

    companion object {
        const val TYPED_VAR_PREFIX: String = "_____"
        val DEFAULT_CONTEXT: PatternContext = PatternContext("default", "Default")
        val PROPERTY_CONTEXT: PatternContext = PatternContext("property", "Top-level / Class property")
        private val PATTERN_CONTEXTS: MutableList<PatternContext> = mutableListOf(DEFAULT_CONTEXT, PROPERTY_CONTEXT)

        fun getNonWhitespaceChildren(fragment: PsiElement): List<PsiElement> {
            var element = fragment.firstChild
            val result: MutableList<PsiElement> = SmartList()
            while (element != null) {
                if (element !is PsiWhiteSpace) result.add(element)
                element = element.nextSibling
            }
            return result
        }
    }
}