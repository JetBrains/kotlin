package com.jetbrains.kotlin.structuralsearch.impl.matcher.compiler

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.structuralsearch.impl.matcher.compiler.GlobalCompilingVisitor
import com.intellij.structuralsearch.impl.matcher.compiler.WordOptimizer
import com.intellij.structuralsearch.impl.matcher.handlers.SubstitutionHandler
import com.intellij.structuralsearch.impl.matcher.handlers.TopLevelMatchingHandler
import com.jetbrains.kotlin.structuralsearch.impl.matcher.KotlinRecursiveElementVisitor
import com.jetbrains.kotlin.structuralsearch.impl.matcher.KotlinRecursiveElementWalkingVisitor
import com.jetbrains.kotlin.structuralsearch.impl.matcher.handlers.DeclarationHandler
import org.jetbrains.kotlin.psi.*

class KotlinCompilingVisitor(private val myCompilingVisitor: GlobalCompilingVisitor) : KotlinRecursiveElementVisitor() {
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

    override fun visitElement(element: PsiElement) {
        super.visitElement(element)
        myCompilingVisitor.handle(element)
    }

    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
        super.visitDotQualifiedExpression(expression)
        myCompilingVisitor.context.pattern.getHandler(expression).setFilter {
            it is KtDotQualifiedExpression || it is KtReferenceExpression
        }
    }

    override fun visitReferenceExpression(expression: KtReferenceExpression) {
        visitElement(expression)
        super.visitReferenceExpression(expression)
    }

    override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
        visitElement(expression)
        myCompilingVisitor.context.pattern.getHandler(expression).let { handler ->
            if (handler is SubstitutionHandler) handler.setFilter { true }
        }
    }

    override fun visitSimpleNameStringTemplateEntry(entry: KtSimpleNameStringTemplateEntry) {
        visitElement(entry)
        val expression = entry.expression ?: return
        val pattern = myCompilingVisitor.context.pattern
        val exprHandler = pattern.getHandler(expression)

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
            pattern.setHandler(entry, newHandler)
        }
    }

    override fun visitDeclaration(dcl: KtDeclaration) {
        super.visitDeclaration(dcl)

        val handler = DeclarationHandler()
        handler.setFilter { it is KtDeclaration || it is PsiComment }
        myCompilingVisitor.context.pattern.setHandler(dcl, handler)

        val lastElement = PsiTreeUtil.skipWhitespacesBackward(dcl)
        if (lastElement is PsiComment) myCompilingVisitor.context.pattern.setHandler(lastElement, handler)
    }
}