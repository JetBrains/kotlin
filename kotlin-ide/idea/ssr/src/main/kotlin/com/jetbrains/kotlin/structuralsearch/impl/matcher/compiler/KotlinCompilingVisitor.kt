package com.jetbrains.kotlin.structuralsearch.impl.matcher.compiler

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
import com.jetbrains.kotlin.structuralsearch.impl.matcher.KotlinMatchUtil
import com.jetbrains.kotlin.structuralsearch.impl.matcher.KotlinRecursiveElementVisitor
import com.jetbrains.kotlin.structuralsearch.impl.matcher.KotlinRecursiveElementWalkingVisitor
import com.jetbrains.kotlin.structuralsearch.impl.matcher.handlers.DeclarationHandler
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.psi.*
import java.util.regex.Pattern

class KotlinCompilingVisitor(private val myCompilingVisitor: GlobalCompilingVisitor) : KotlinRecursiveElementVisitor() {

    private val ourSubstitutionPattern = Pattern.compile("\\b(_____\\w+)\\b")

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

    private fun getHandler(element: PsiElement) =
        myCompilingVisitor.context.pattern.getHandler(element)

    private fun setHandler(element: PsiElement, handler: MatchingHandler) =
        myCompilingVisitor.context.pattern.setHandler(element, handler)

    override fun visitElement(element: PsiElement) {
        super.visitElement(element)
        myCompilingVisitor.handle(element)

        when (element) {
            is LeafPsiElement -> getHandler(element).setFilter { it is LeafPsiElement }
            is KDoc -> getHandler(element).setFilter { it is KDoc }
        }
    }

    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
        super.visitDotQualifiedExpression(expression)
        getHandler(expression).setFilter {
            it is KtDotQualifiedExpression || it is KtReferenceExpression
        }
    }

    override fun visitReferenceExpression(expression: KtReferenceExpression) {
        visitElement(expression)
        super.visitReferenceExpression(expression)
    }

    override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
        visitElement(expression)
        getHandler(expression).let { handler ->
            if (handler is SubstitutionHandler) handler.setFilter { true }
        }
    }

    override fun visitLiteralStringTemplateEntry(entry: KtLiteralStringTemplateEntry) {
        super.visitLiteralStringTemplateEntry(entry)

        if (ourSubstitutionPattern.matcher(entry.text).find()) {
            myCompilingVisitor.processPatternStringWithFragments(entry.text, COMMENT, ourSubstitutionPattern)?.let {
                entry.putUserData(CompiledPattern.HANDLER_KEY, it)
            }
        }
        getHandler(entry).setFilter { it is KtLiteralStringTemplateEntry }
    }

    override fun visitSimpleNameStringTemplateEntry(entry: KtSimpleNameStringTemplateEntry) {
        visitElement(entry)
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

        val handler = DeclarationHandler()
        handler.setFilter { it is KtDeclaration || it is PsiComment }
        setHandler(dcl, handler)

        val lastElement = PsiTreeUtil.skipWhitespacesBackward(dcl)
        if (lastElement is PsiComment) setHandler(lastElement, handler)
    }

    override fun visitComment(comment: PsiComment) {
        super.visitComment(comment)
        val text = KotlinMatchUtil.getCommentText(comment).trim()

        if (ourSubstitutionPattern.matcher(text).find()) {
            myCompilingVisitor.processPatternStringWithFragments(comment.text, COMMENT, ourSubstitutionPattern)?.let {
                comment.putUserData(CompiledPattern.HANDLER_KEY, it)
            }
        }
    }
}