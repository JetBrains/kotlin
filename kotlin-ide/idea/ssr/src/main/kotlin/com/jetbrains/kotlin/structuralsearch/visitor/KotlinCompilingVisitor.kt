package com.jetbrains.kotlin.structuralsearch.visitor

import com.intellij.dupLocator.util.NodeFilter
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.structuralsearch.impl.matcher.CompiledPattern
import com.intellij.structuralsearch.impl.matcher.compiler.GlobalCompilingVisitor
import com.intellij.structuralsearch.impl.matcher.compiler.GlobalCompilingVisitor.OccurenceKind.COMMENT
import com.intellij.structuralsearch.impl.matcher.compiler.WordOptimizer
import com.intellij.structuralsearch.impl.matcher.handlers.MatchingHandler
import com.intellij.structuralsearch.impl.matcher.handlers.SubstitutionHandler
import com.intellij.structuralsearch.impl.matcher.handlers.TopLevelMatchingHandler
import com.jetbrains.kotlin.structuralsearch.getCommentText
import com.jetbrains.kotlin.structuralsearch.handler.CommentedDeclarationHandler
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.kdoc.psi.impl.KDocLink
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import java.util.regex.Pattern

class KotlinCompilingVisitor(private val myCompilingVisitor: GlobalCompilingVisitor) : KotlinRecursiveElementVisitor() {
    private val mySubstitutionPattern = Pattern.compile("\\b(_____\\w+)\\b")

    fun compile(topLevelElements: Array<out PsiElement>?) {
        val optimizer = KotlinWordOptimizer()
        val pattern = myCompilingVisitor.context.pattern
        if (topLevelElements == null) return

        for (element in topLevelElements) {
            element.accept(this)
            element.accept(optimizer)
            pattern.setHandler(element, TopLevelMatchingHandler(pattern.getHandler(element)))
        }
    }

    inner class KotlinWordOptimizer : KotlinRecursiveElementWalkingVisitor(), WordOptimizer

    private fun getHandler(element: PsiElement) = myCompilingVisitor.context.pattern.getHandler(element)

    private fun setHandler(element: PsiElement, handler: MatchingHandler) =
        myCompilingVisitor.context.pattern.setHandler(element, handler)

    private fun processPatternStringWithFragments(element: PsiElement, text: String = element.text) {
        if (mySubstitutionPattern.matcher(text).find()) {
            myCompilingVisitor.processPatternStringWithFragments(element.text, COMMENT, mySubstitutionPattern)?.let {
                element.putUserData(CompiledPattern.HANDLER_KEY, it)
            }
        }
    }

    override fun visitElement(element: PsiElement) {
        myCompilingVisitor.handle(element)
        when (element) {
            is LeafPsiElement -> visitLeafPsiElement(element)
            is KDoc -> visitKDoc(element)
            is KDocLink -> visitKDocLink(element)
        }

        super.visitElement(element)
    }

    override fun visitReferenceExpression(expression: KtReferenceExpression) {
        visitElement(expression)
        if (getHandler(expression) is SubstitutionHandler) getHandler(expression).setFilter { it is PsiElement }
    }

    private fun visitLeafPsiElement(element: LeafPsiElement) {
        getHandler(element).setFilter { it is LeafPsiElement }

        when (element.elementType) {
            KDocTokens.TEXT, KDocTokens.TAG_NAME -> processPatternStringWithFragments(element)
        }
    }

    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
        super.visitDotQualifiedExpression(expression)
        getHandler(expression).filter = DotQualifiedExpressionFilter
    }

    override fun visitBinaryExpression(expression: KtBinaryExpression) {
        super.visitBinaryExpression(expression)
        getHandler(expression).filter = BinaryExpressionFilter
    }

    override fun visitUnaryExpression(expression: KtUnaryExpression) {
        super.visitUnaryExpression(expression)
        getHandler(expression).filter = UnaryExpressionFilter
    }

    override fun visitArrayAccessExpression(expression: KtArrayAccessExpression) {
        super.visitArrayAccessExpression(expression)
        getHandler(expression).filter = ArrayAccessExpressionFilter
    }

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        getHandler(expression).filter = CallExpressionFilter
    }

    override fun visitConstantExpression(expression: KtConstantExpression) {
        super.visitConstantExpression(expression)
        getHandler(expression).filter = ConstantExpressionFilter
    }

    override fun visitLiteralStringTemplateEntry(entry: KtLiteralStringTemplateEntry) {
        super.visitLiteralStringTemplateEntry(entry)
        processPatternStringWithFragments(entry)
        getHandler(entry).setFilter { it is KtLiteralStringTemplateEntry }
    }

    override fun visitSimpleNameStringTemplateEntry(entry: KtSimpleNameStringTemplateEntry) {
        super.visitSimpleNameStringTemplateEntry(entry)
        getHandler(entry).filter = SimpleNameSTEFilter

        val expression = entry.expression ?: return
        val exprHandler = getHandler(expression)

        // Apply the child SubstitutionHandler to the TemplateEntry
        if (exprHandler is SubstitutionHandler) {
            val newHandler = SubstitutionHandler(
                "${exprHandler.name}_parent",
                false,
                exprHandler.minOccurs,
                exprHandler.maxOccurs,
                true
            ).apply {
                setFilter { it is KtStringTemplateEntry }
                val exprPredicate = exprHandler.predicate
                if (exprPredicate != null) predicate = exprPredicate
            }
            setHandler(entry, newHandler)
        }
    }

    override fun visitDeclaration(dcl: KtDeclaration) {
        super.visitDeclaration(dcl)
        getHandler(dcl).filter = DeclarationFilter

        if (dcl.getChildOfType<PsiComment>() != null || PsiTreeUtil.skipWhitespacesBackward(dcl) is PsiComment) {
            val handler = CommentedDeclarationHandler()
            handler.filter = CommentedDeclarationFilter
            setHandler(dcl, handler)
        }
    }

    override fun visitParameter(parameter: KtParameter) {
        super.visitParameter(parameter)
        getHandler(parameter).filter = ParameterFilter
    }

    override fun visitComment(comment: PsiComment) {
        super.visitComment(comment)
        processPatternStringWithFragments(
            comment, getCommentText(comment)
                .trim()
        )

        if (comment.parent is KtDeclaration || PsiTreeUtil.skipWhitespacesForward(comment) is KtDeclaration) {
            val handler = CommentedDeclarationHandler()
            handler.filter = CommentedDeclarationFilter
            setHandler(comment, handler)
        }
    }

    private fun visitKDoc(kDoc: KDoc) {
        getHandler(kDoc).setFilter { it is KDoc }
    }

    private fun visitKDocLink(link: KDocLink) {
        getHandler(link).setFilter { it is KDocLink }
    }

    companion object {
        val ArrayAccessExpressionFilter: NodeFilter = NodeFilter {
            it is KtArrayAccessExpression || it is KtDotQualifiedExpression
        }

        /** translated op matching */
        val CallExpressionFilter: NodeFilter = NodeFilter {
            it is KtCallExpression || it is KtDotQualifiedExpression
        }

        /** translated op matching */
        val UnaryExpressionFilter: NodeFilter = NodeFilter {
            it is KtUnaryExpression || it is KtDotQualifiedExpression
        }

        val BinaryExpressionFilter: NodeFilter = NodeFilter {
            it is KtBinaryExpression || it is KtDotQualifiedExpression || it is KtPrefixExpression
        }

        val ConstantExpressionFilter: NodeFilter = NodeFilter {
            it is KtConstantExpression || it is KtParenthesizedExpression
        }

        val DotQualifiedExpressionFilter: NodeFilter = NodeFilter {
            it is KtDotQualifiedExpression || it is KtReferenceExpression
        }

        val ParameterFilter: NodeFilter = NodeFilter {
            it is KtDeclaration || it is KtUserType || it is KtNameReferenceExpression
        }

        val DeclarationFilter: NodeFilter = NodeFilter {
            it is KtDeclaration || it is KtTypeProjection || it is KtTypeElement || it is KtNameReferenceExpression
        }

        val CommentedDeclarationFilter: NodeFilter = NodeFilter {
            it is PsiComment || DeclarationFilter.accepts(it)
        }

        val SimpleNameSTEFilter: NodeFilter = NodeFilter {
            it is KtSimpleNameStringTemplateEntry || it is KtBlockStringTemplateEntry
        }
    }
}