package com.jetbrains.kotlin.structuralsearch

import com.intellij.dupLocator.util.NodeFilter
import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.elementType
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
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
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

        override fun getTypedVarString(element: PsiElement): String {
            val typedVarString = super.getTypedVarString(element)
            return if (typedVarString.firstOrNull() == '@') typedVarString.drop(1) else typedVarString
        }
    }

    override fun isMyLanguage(language: Language): Boolean = language == KotlinLanguage.INSTANCE

    override fun getTemplateContextTypeClass(): Class<KotlinTemplateContextType> = KotlinTemplateContextType::class.java

    override fun getPredefinedTemplates(): Array<Configuration> = KotlinPredefinedConfigurations.createPredefinedTemplates()

    override fun getDefaultFileType(fileType: LanguageFileType?): LanguageFileType = fileType ?: KotlinFileType.INSTANCE

    override fun supportsShortenFQNames(): Boolean = true

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
        var elements: List<PsiElement>
        if (PROPERTY_CONTEXT.id == contextId) {
            try {
                val fragment = KtPsiFactory(project, false).createProperty(text)
                elements = listOf(getNonWhitespaceChildren(fragment).first().parent)
                if (elements.first() !is KtProperty) return PsiElement.EMPTY_ARRAY
            } catch (e: Exception) {
                return arrayOf(KtPsiFactory(project, false).createComment("//").apply {
                    putUserData(PATTERN_ERROR, KSSRBundle.message("error.context.getter.or.setter"))
                })
            }
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

//        for (element in elements) print(DebugUtil.psiToString(element, false))

        return elements.toTypedArray()
    }

    inner class KotlinValidator : KotlinRecursiveElementWalkingVisitor() {
        override fun visitErrorElement(element: PsiErrorElement) {
            super.visitErrorElement(element)
            if (shouldShowProblem(element)) {
                throw MalformedPatternException(element.errorDescription)
            }
        }

        override fun visitComment(comment: PsiComment) {
            super.visitComment(comment)
            comment.getUserData(PATTERN_ERROR)?.let { error ->
                throw MalformedPatternException(error)
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

    private fun ancestors(node: PsiElement?): List<PsiElement?> {
        val family = mutableListOf(node)
        repeat(7) { family.add(family.last()?.parent) }
        return family.drop(1)
    }

    override fun isApplicableConstraint(
        constraintName: String,
        variableNode: PsiElement?,
        completePattern: Boolean,
        target: Boolean
    ): Boolean {
        if (variableNode != null)
            return when (constraintName) {
                UIUtil.TYPE, UIUtil.TYPE_REGEX -> isApplicableType(variableNode)
                UIUtil.MINIMUM_ZERO -> isApplicableMinCount(variableNode) || isApplicableMinMaxCount(variableNode)
                UIUtil.MAXIMUM_UNLIMITED -> isApplicableMaxCount(variableNode) || isApplicableMinMaxCount(variableNode)
                UIUtil.TEXT_HIERARCHY -> isApplicableTextHierarchy(variableNode)
                UIUtil.REFERENCE -> isApplicableReference(variableNode)
                else -> super.isApplicableConstraint(constraintName, variableNode, completePattern, target)
            }

        return super.isApplicableConstraint(constraintName, null as PsiElement?, completePattern, target)
    }

    private fun isApplicableReference(variableNode: PsiElement): Boolean = variableNode.parent is KtNameReferenceExpression

    private fun isApplicableTextHierarchy(variableNode: PsiElement): Boolean {
        val family = ancestors(variableNode)
        return when {
            family[0] is KtClassOrObject && (family[0] as KtClass).nameIdentifier == variableNode -> true
            family[0] is KtNamedDeclaration && family[2] is KtClassOrObject -> true
            family[3] is KtSuperTypeListEntry && family[5] is KtClassOrObject -> true
            family[4] is KtSuperTypeListEntry && family[6] is KtClassOrObject -> true
            else -> false
        }
    }

    private fun isApplicableType(variableNode: PsiElement): Boolean {
        val family = ancestors(variableNode)
        return when {
            family[0] is KtNameReferenceExpression -> when (family[1]) {
                is KtValueArgument,
                is KtProperty,
                is KtBinaryExpression, is KtBinaryExpressionWithTypeRHS,
                is KtIsExpression,
                is KtBlockExpression,
                is KtContainerNode,
                is KtArrayAccessExpression,
                is KtPostfixExpression,
                is KtDotQualifiedExpression,
                is KtSafeQualifiedExpression,
                is KtCallableReferenceExpression,
                is KtSimpleNameStringTemplateEntry, is KtBlockStringTemplateEntry,
                is KtPropertyAccessor,
                is KtWhenEntry -> true
                else -> false
            }
            family[0] is KtProperty -> true
            family[0] is KtParameter -> true
            else -> false
        }
    }

    /**
     * Returns true if the largest count filter should be [0; 1].
     */
    private fun isApplicableMinCount(variableNode: PsiElement): Boolean {
        val family = ancestors(variableNode)
        return when {
            family[0] !is KtNameReferenceExpression -> false
            family[1] is KtProperty -> true
            family[1] is KtDotQualifiedExpression -> true
            family[1] is KtCallableReferenceExpression && family[0]?.nextSibling.elementType == KtTokens.COLONCOLON -> true
            family[1] is KtWhenExpression -> true
            family[2] is KtTypeReference && family[3] is KtNamedFunction -> true
            family[3] is KtConstructorCalleeExpression -> true
            else -> false
        }
    }

    /**
     * Returns true if the largest count filter should be [1; +inf].
     */
    private fun isApplicableMaxCount(variableNode: PsiElement): Boolean {
        val family = ancestors(variableNode)
        return when {
            family[0] is KtDestructuringDeclarationEntry -> true
            family[0] is KtNameReferenceExpression && family[1] is KtWhenConditionWithExpression -> true
            else -> false
        }
    }

    /**
     * Returns true if the largest count filter should be [0; +inf].
     */
    private fun isApplicableMinMaxCount(variableNode: PsiElement): Boolean {
        val family = ancestors(variableNode)
//        println(family.map { if (it == null) "null" else it::class.java.toString().split(".").last() })
        return when {
            // Containers (lists, bodies, ...)
            family[1] is KtClassBody -> true
            family[0] is KtParameter && family[1] is KtParameterList -> true
            family[0] is KtTypeParameter && family[1] is KtTypeParameterList -> true
            family[2] is KtTypeParameter && family[3] is KtTypeParameterList -> true
            family[1] is KtUserType && family[4] is KtParameterList && family[5] !is KtNamedFunction -> true
            family[1] is KtUserType && family[3] is KtSuperTypeEntry -> true
            family[1] is KtValueArgument && family[2] is KtValueArgumentList -> true
            family[1] is KtBlockExpression && family[3] is KtDoWhileExpression -> true
            family[0] is KtNameReferenceExpression && family[1] is KtBlockExpression -> true
            family[1] is KtUserType && family[3] is KtTypeProjection && family[5] !is KtNamedFunction -> true
            // Annotations
            family[1] is KtUserType && family[4] is KtAnnotationEntry -> true
            family[1] is KtCollectionLiteralExpression -> true
            // Strings
            family[1] is KtSimpleNameStringTemplateEntry -> true
            // KDoc
            family[0] is KDocTag -> true
            // Default: count filter not applicable
            else -> false
        }
    }

    override fun getCustomPredicates(
        constraint: MatchVariableConstraint?,
        name: String,
        options: MatchOptions
    ): MutableList<MatchPredicate> {
        val result = SmartList<MatchPredicate>()
        constraint?.apply {
            if (!StringUtil.isEmptyOrSpaces(nameOfExprType)) {
                val predicate = KotlinExprTypePredicate(
                    search = if (isRegexExprType) nameOfExprType else expressionTypes,
                    withinHierarchy = isExprTypeWithinHierarchy,
                    ignoreCase = !options.isCaseSensitiveMatch,
                    target = isPartOfSearchResults,
                    baseName = name,
                    regex = isRegexExprType
                )
                result.add(if (isInvertExprType) NotPredicate(predicate) else predicate)
            }
        }
        return result
    }

    private fun isProbableExpression(pattern: String, fileType: LanguageFileType, dialect: Language, project: Project): Boolean {
        val searchElements = createPatternTree(pattern, PatternTreeContext.Block, fileType, dialect, null, project, false)
        return searchElements[0] is KtDeclaration
    }

    override fun getReplaceHandler(project: Project, replaceOptions: ReplaceOptions): KotlinReplaceHandler =
        KotlinReplaceHandler(project)

    override fun getPatternContexts(): MutableList<PatternContext> = PATTERN_CONTEXTS

    companion object {
        const val TYPED_VAR_PREFIX: String = "_____"
        val DEFAULT_CONTEXT: PatternContext = PatternContext("default", KSSRBundle.message("context.default"))
        val PROPERTY_CONTEXT: PatternContext = PatternContext("property", KSSRBundle.message("context.property.getter.or.setter"))
        private val PATTERN_CONTEXTS: MutableList<PatternContext> = mutableListOf(DEFAULT_CONTEXT, PROPERTY_CONTEXT)
        private val PATTERN_ERROR: Key<String> = Key("patternError")

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