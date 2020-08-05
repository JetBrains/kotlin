package com.jetbrains.kotlin.structuralsearch

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.tree.IElementType
import com.intellij.structuralsearch.StructuralSearchUtil
import com.intellij.structuralsearch.impl.matcher.MatchContext
import com.intellij.structuralsearch.impl.matcher.predicates.MatchPredicate
import com.intellij.structuralsearch.impl.matcher.predicates.RegExpPredicate
import org.jetbrains.kotlin.cfg.pseudocode.getSubtypesPredicate
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.diagnostics.rendering.Renderers
import org.jetbrains.kotlin.idea.debugger.sequence.psi.resolveType
import org.jetbrains.kotlin.idea.refactoring.fqName.fqName
import org.jetbrains.kotlin.idea.search.allScope
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.idea.stubindex.KotlinClassShortNameIndex
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.lexer.KotlinLexer
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.nj2k.postProcessing.type
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtSimpleNameStringTemplateEntry
import org.jetbrains.kotlin.psi.KtStringTemplateEntry
import org.jetbrains.kotlin.psi.psiUtil.isNull
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.typeUtil.immediateSupertypes
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import org.jetbrains.kotlin.types.typeUtil.supertypes
import org.jetbrains.kotlinx.serialization.compiler.resolve.toSimpleType

class KotlinExprTypePredicate(
    private val search: String,
    private val withinHierarchy: Boolean,
    private val ignoreCase: Boolean,
    private val target: Boolean,
    private val baseName: String,
    private val regex: Boolean
) : MatchPredicate() {

    override fun match(matchedNode: PsiElement, start: Int, end: Int, context: MatchContext): Boolean {
        val searchedTypeNames = if (regex) listOf() else search.split('|')
        if (matchedNode is KtExpression && matchedNode.isNull() && searchedTypeNames.contains("null")) return true
        val node = StructuralSearchUtil.getParentIfIdentifier(matchedNode)
        val type = when {
            node is KtDeclaration -> node.type()
            node is KtExpression -> try {
                node.resolveType()
            } catch (e: Exception) {
                null
            }
            node is KtStringTemplateEntry && node !is KtSimpleNameStringTemplateEntry -> null
            node is KtSimpleNameStringTemplateEntry -> node.expression?.resolveType()
            else -> throw IllegalStateException(KSSRBundle.message("error.type.filter.node"))
        } ?: return false

        val project = node.project
        val scope = project.allScope()

        if (regex) {
            val delegate = RegExpPredicate(search, !ignoreCase, baseName, false, target)
            val typesToTest = mutableListOf(type)
            if (withinHierarchy) typesToTest.addAll(type.supertypes())

            return typesToTest.any {
                delegate.doMatch(DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(it), context, matchedNode)
                        || delegate.doMatch(DescriptorRenderer.FQ_NAMES_IN_TYPES.renderType(it), context, matchedNode)
            }
        }

        return searchedTypeNames
            .filter { it != "null" }
            .mapNotNull(Type.Companion::fromString)
            .any { it.compareWith(type, project, scope, withinHierarchy) }
    }

    class Type {
        var className: String = ""
        var arguments: MutableList<Type> = mutableListOf()
        var nullable: Boolean = false
        var variance: KtToken? = null
        var functionType: Boolean = false

        fun compareWith(type: KotlinType, project: Project, scope: GlobalSearchScope, withinHierarchy: Boolean): Boolean {
            val index =
                if (className.contains(".")) KotlinFullClassNameIndex.getInstance()
                else KotlinClassShortNameIndex.getInstance()

            if (functionType) {
                if (type.fqName.toString() == className && type.isMarkedNullable == nullable) return (0..arguments.lastIndex).all { i ->
                    arguments[i].compareWith(type.arguments[i].type, project, scope, false)
                }
            } else
                index.get(className, project, scope).forEach {
                    val searchedKotlinClass = (it.descriptor as ClassDescriptor).toSimpleType(nullable)
                    if (searchedKotlinClass.fqName == type.fqName && searchedKotlinClass.isMarkedNullable == type.isMarkedNullable
                        || withinHierarchy && type.isSubtypeOf(searchedKotlinClass)
                    ) {
                        if (arguments.any()) {
                            if (arguments.size != type.arguments.size) return@compareWith false
                            return@compareWith (0..arguments.lastIndex).all { i ->
                                compareVariance(arguments[i].variance, type.arguments[i].projectionKind)
                                        && arguments[i].compareWith(type.arguments[i].type, project, scope, false)
                            }
                        }
                        return@compareWith true
                    }
                }
            return false
        }

        private fun compareVariance(variance: KtToken?, projectionKind: Variance) = when (projectionKind) {
            Variance.INVARIANT -> variance == null
            Variance.IN_VARIANCE -> variance == KtTokens.IN_KEYWORD
            Variance.OUT_VARIANCE -> variance == KtTokens.OUT_KEYWORD
        }

        companion object {
            fun fromString(string: String): Type? {
                val lex = KotlinLexer()
                val buf = "var x : $string"
                lex.start(buf, 8, buf.length)
                return getType(lex)
            }

            private fun getType(lexer: KotlinLexer): Type? =
                if (lexer.nextUseful == KtTokens.LPAR) getFunctionType(lexer) else getRegularType(lexer)

            private fun getFunctionType(lexer: KotlinLexer): Type? {
                val typeList = getTypeList(lexer, KtTokens.LPAR to KtTokens.RPAR) ?: return null

                if (lexer.nextUseful != KtTokens.ARROW) return null
                lexer.advance()

                val destinationType = getType(lexer) ?: return null

                return Type().apply {
                    className = "kotlin.Function${typeList.size}"
                    arguments.addAll(typeList)
                    arguments.add(destinationType)
                    if (lexer.nextUseful == KtTokens.QUEST) {
                        nullable = true
                        lexer.advance()
                    }
                    functionType = true
                }
            }

            private fun getRegularType(lexer: KotlinLexer): Type? {
                var variance: KtToken? = null

                // out is not recognised as OUT_KEYWORD so we need to use lexer.tokenText
                while (lexer.tokenType == KtTokens.WHITE_SPACE) lexer.advance()
                val text = lexer.tokenText
                lexer.advance()
                val identifier: String
                if ((text == "in" || text == "out") && lexer.tokenType == KtTokens.WHITE_SPACE) {
                    variance = if (text == "in") KtTokens.IN_KEYWORD else KtTokens.OUT_KEYWORD
                    identifier = getIdentifier(lexer)
                } else {
                    identifier = text + getIdentifier(lexer)
                }

                val arguments = getTypeList(lexer, KtTokens.LT to KtTokens.GT) ?: return null

                var nullable = false
                if (lexer.nextUseful == KtTokens.QUEST) {
                    nullable = true
                    lexer.advance()
                }

                return Type().apply {
                    className = identifier
                    this.arguments.addAll(arguments)
                    this.nullable = nullable
                    this.variance = variance
                }
            }

            private fun getTypeList(lexer: KotlinLexer, limits: Pair<KtToken, KtToken>): List<Type>? {
                val arguments = mutableListOf<Type>()
                if (lexer.nextUseful == limits.first) {
                    lexer.advance()
                    while (lexer.nextUseful != limits.second) {
                        arguments.add(getType(lexer) ?: return null)
                        if (lexer.nextUseful == KtTokens.COMMA) lexer.advance()
                    }
                    lexer.advance()
                }
                return arguments
            }

            private fun getIdentifier(lexer: KotlinLexer): String {
                var identifier = ""
                while (lexer.tokenType == KtTokens.WHITE_SPACE) lexer.advance()
                while (lexer.tokenType == KtTokens.IDENTIFIER || lexer.tokenType == KtTokens.DOT) {
                    identifier += lexer.tokenText
                    lexer.advance()
                }
                return identifier
            }

            private val KotlinLexer.nextUseful: IElementType?
                get() {
                    while (tokenType == KtTokens.WHITE_SPACE) advance()
                    return tokenType
                }
        }
    }

}