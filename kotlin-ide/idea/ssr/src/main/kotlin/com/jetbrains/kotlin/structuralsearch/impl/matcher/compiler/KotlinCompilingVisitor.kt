package com.jetbrains.kotlin.structuralsearch.impl.matcher.compiler

import com.intellij.psi.PsiElement
import com.intellij.structuralsearch.StructuralSearchUtil
import com.intellij.structuralsearch.impl.matcher.compiler.GlobalCompilingVisitor
import com.intellij.structuralsearch.impl.matcher.compiler.WordOptimizer
import com.intellij.structuralsearch.impl.matcher.handlers.SubstitutionHandler
import com.intellij.structuralsearch.impl.matcher.handlers.TopLevelMatchingHandler
import com.intellij.structuralsearch.impl.matcher.predicates.RegExpPredicate
import com.jetbrains.kotlin.structuralsearch.impl.matcher.KotlinCompiledPattern
import com.jetbrains.kotlin.structuralsearch.impl.matcher.KotlinRecursiveElementVisitor
import com.jetbrains.kotlin.structuralsearch.impl.matcher.KotlinRecursiveElementWalkingVisitor
import org.jetbrains.kotlin.nj2k.postProcessing.resolve
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression

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

    override fun visitReferenceExpression(reference: KtReferenceExpression) {
        visitElement(reference)
        super.visitReferenceExpression(reference)
    }

    override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
        visitElement(expression)
        val parent = expression.parent
        val pattern = myCompilingVisitor.context.pattern
        val handler = pattern.getHandler(expression)

        // Sets a SubstitutionHandler if TYPED_VAR_PREFIX is recognized
        if (handler !is SubstitutionHandler && expression.getReferencedName()
                .startsWith(KotlinCompiledPattern.TYPED_VAR_PREFIX)
        ) {
            val resolve = expression.resolve()
            val text = if (resolve != null) {
                (resolve as KtClass).name ?: expression.text
            } else {
                expression.text
            }
            createAndSetSubstitutionHandlerFromReference(expression, text, parent is KtReferenceExpression)
        }
    }

    private fun createAndSetSubstitutionHandlerFromReference(
        expr: PsiElement,
        referenceText: String,
        classQualifier: Boolean
    ) {
        val substitutionHandler =
            SubstitutionHandler("__${referenceText.replace('.', '_')}", false, if (classQualifier) 0 else 1, 1, true)
        val caseSensitive = myCompilingVisitor.context.options.isCaseSensitiveMatch
        substitutionHandler.predicate = RegExpPredicate(
            StructuralSearchUtil.shieldRegExpMetaChars(referenceText),
            caseSensitive,
            null,
            false,
            false
        )
        myCompilingVisitor.context.pattern.setHandler(expr, substitutionHandler)
    }
}