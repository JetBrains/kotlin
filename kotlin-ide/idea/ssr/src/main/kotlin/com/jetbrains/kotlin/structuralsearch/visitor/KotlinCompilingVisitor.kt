package com.jetbrains.kotlin.structuralsearch.visitor

import com.intellij.dupLocator.util.NodeFilter
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.structuralsearch.impl.matcher.CompiledPattern
import com.intellij.structuralsearch.impl.matcher.compiler.GlobalCompilingVisitor
import com.intellij.structuralsearch.impl.matcher.compiler.GlobalCompilingVisitor.OccurenceKind.CODE
import com.intellij.structuralsearch.impl.matcher.compiler.GlobalCompilingVisitor.OccurenceKind.COMMENT
import com.intellij.structuralsearch.impl.matcher.compiler.WordOptimizer
import com.intellij.structuralsearch.impl.matcher.handlers.MatchingHandler
import com.intellij.structuralsearch.impl.matcher.handlers.SubstitutionHandler
import com.intellij.structuralsearch.impl.matcher.handlers.TopLevelMatchingHandler
import com.jetbrains.kotlin.structuralsearch.getCommentText
import com.jetbrains.kotlin.structuralsearch.handler.CommentedDeclarationHandler
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.kdoc.psi.impl.KDocLink
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.referenceExpression
import org.jetbrains.kotlin.psi2ir.deparenthesize
import org.jetbrains.kotlin.resolve.calls.callUtil.getCalleeExpressionIfAny
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

    inner class KotlinWordOptimizer : KotlinRecursiveElementWalkingVisitor(), WordOptimizer {

        override fun visitClass(klass: KtClass) {
            if (!handleWord(klass.name, CODE, myCompilingVisitor.context)) return
            super.visitClass(klass)
        }

        override fun visitNamedFunction(function: KtNamedFunction) {
            if (!handleWord(function.name, CODE, myCompilingVisitor.context)) return
            super.visitNamedFunction(function)
        }

        override fun visitParameter(parameter: KtParameter) {
            if (!handleWord(parameter.name, CODE, myCompilingVisitor.context)) return
            super.visitParameter(parameter)
        }

        override fun visitProperty(property: KtProperty) {
            if (!handleWord(property.name, CODE, myCompilingVisitor.context)) return
            super.visitProperty(property)
        }

        override fun visitConstantExpression(expression: KtConstantExpression) {
            val type = expression.elementType
            if (type == KtNodeTypes.BOOLEAN_CONSTANT || type == KtNodeTypes.NULL)
                if (!handleWord(expression.text, CODE, myCompilingVisitor.context)) return
            super.visitConstantExpression(expression)
        }
    }

    private fun getHandler(element: PsiElement) = myCompilingVisitor.context.pattern.getHandler(element)

    private fun setHandler(element: PsiElement, handler: MatchingHandler) =
        myCompilingVisitor.context.pattern.setHandler(element, handler)

    private fun processPatternStringWithFragments(element: PsiElement, text: String = element.text) {
        if (mySubstitutionPattern.matcher(text).find()) {
            myCompilingVisitor.processPatternStringWithFragments(text, COMMENT, mySubstitutionPattern)?.let {
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
        val handler = getHandler(expression)
        getHandler(expression).filter =
            if (handler is SubstitutionHandler) NodeFilter { it is PsiElement } // accept all
            else ReferenceExpressionFilter
    }

    private fun visitLeafPsiElement(element: LeafPsiElement) {
        getHandler(element).setFilter { it is LeafPsiElement }

        when (element.elementType) {
            KDocTokens.TEXT, KDocTokens.TAG_NAME -> processPatternStringWithFragments(element)
        }
    }

    override fun visitExpression(expression: KtExpression) {
        super.visitExpression(expression)
        getHandler(expression).filter = ExpressionFilter
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
        parameter.typeReference?.resetCountFilter()
        parameter.typeReference?.typeElement?.resetCountFilter()
        parameter.typeReference?.typeElement?.firstChild?.resetCountFilter()
        parameter.typeReference?.typeElement?.firstChild?.firstChild?.resetCountFilter()
    }

    override fun visitComment(comment: PsiComment) {
        super.visitComment(comment)
        processPatternStringWithFragments(comment, getCommentText(comment).trim())
        getHandler(comment).setFilter { it is PsiComment }

        if (comment.parent is KtDeclaration || PsiTreeUtil.skipWhitespacesForward(comment) is KtDeclaration) {
            val handler = CommentedDeclarationHandler()
            handler.filter = CommentedDeclarationFilter
            setHandler(comment, handler)
        }
    }

    override fun visitAnnotationEntry(annotationEntry: KtAnnotationEntry) {
        super.visitAnnotationEntry(annotationEntry)
        val calleeExpression = annotationEntry.calleeExpression ?: return
        val handler = getHandler(calleeExpression)
        if (handler is SubstitutionHandler) {
            setHandler(
                annotationEntry, SubstitutionHandler(
                    "${handler.name}_",
                    false,
                    handler.minOccurs,
                    handler.maxOccurs,
                    false
                )
            )
            calleeExpression.resetCountFilter()
            calleeExpression.constructorReferenceExpression?.resetCountFilter()
        }
    }

    override fun visitSuperTypeEntry(specifier: KtSuperTypeEntry) {
        super.visitSuperTypeEntry(specifier)
        specifier.typeReference?.resetCountFilter()
        specifier.typeReference?.typeElement?.resetCountFilter()
    }

    override fun visitModifierList(list: KtModifierList) {
        super.visitModifierList(list)
        if (list.allChildren.all { it.allowsAbsenceOfMatch }) {
            setHandler(list, absenceOfMatchHandler(list))
        }
    }

    override fun visitParameterList(list: KtParameterList) {
        super.visitParameterList(list)
        if (list.children.all { it.allowsAbsenceOfMatch }) {
            setHandler(list, absenceOfMatchHandler(list))
        }
    }

    override fun visitValueArgumentList(list: KtValueArgumentList) {
        super.visitValueArgumentList(list)
        if (list.children.all { it.allowsAbsenceOfMatch }) {
            setHandler(list, absenceOfMatchHandler(list))
        }
    }

    override fun visitClassBody(classBody: KtClassBody) {
        super.visitClassBody(classBody)
        if (classBody.children.all { it.allowsAbsenceOfMatch }) {
            setHandler(classBody, absenceOfMatchHandler(classBody))
        }
    }

    override fun visitPrimaryConstructor(constructor: KtPrimaryConstructor) {
        super.visitPrimaryConstructor(constructor)
        if (constructor.children.all { it.allowsAbsenceOfMatch }) {
            setHandler(constructor, absenceOfMatchHandler(constructor))
        }
    }

    override fun visitSuperTypeList(list: KtSuperTypeList) {
        super.visitSuperTypeList(list)
        if (list.children.all { it.allowsAbsenceOfMatch }) {
            setHandler(list, absenceOfMatchHandler(list))
        }
    }

    private fun visitKDoc(kDoc: KDoc) {
        getHandler(kDoc).setFilter { it is KDoc }
    }

    private fun visitKDocLink(link: KDocLink) {
        getHandler(link).setFilter { it is KDocLink }
    }

    private fun absenceOfMatchHandler(element: PsiElement): SubstitutionHandler =
        SubstitutionHandler("${element.hashCode()}_optional", false, 0, 1, false)

    private val PsiElement.allowsAbsenceOfMatch: Boolean
        get() {
            val handler = getHandler(this)
            return handler is SubstitutionHandler && handler.minOccurs == 0
        }

    private fun PsiElement.resetCountFilter() {
        val handler = getHandler(this)
        if (handler is SubstitutionHandler && (handler.minOccurs != 1 || handler.maxOccurs != 1)) {
            val newHandler = SubstitutionHandler(handler.name, false, 1, 1, false)
            val predicate = handler.predicate
            if (predicate != null) newHandler.predicate = predicate
            setHandler(this, newHandler)
        }
    }


    companion object {

        private fun deparIfNecessary(element: PsiElement): PsiElement =
            if (element is KtParenthesizedExpression) element.deparenthesize() else element

        val ExpressionFilter: NodeFilter = NodeFilter {
            val element = deparIfNecessary(it)
            element is KtExpression
        }

        val ArrayAccessExpressionFilter: NodeFilter = NodeFilter {
            val element = deparIfNecessary(it)
            element is KtArrayAccessExpression || element is KtDotQualifiedExpression
        }

        /** translated op matching */
        val CallExpressionFilter: NodeFilter = NodeFilter {
            val element = deparIfNecessary(it)
            element is KtCallExpression || element is KtDotQualifiedExpression
        }

        /** translated op matching */
        val UnaryExpressionFilter: NodeFilter = NodeFilter {
            val element = deparIfNecessary(it)
            element is KtUnaryExpression || element is KtDotQualifiedExpression
        }

        val BinaryExpressionFilter: NodeFilter = NodeFilter {
            val element = deparIfNecessary(it)
            element is KtBinaryExpression || element is KtDotQualifiedExpression || element is KtPrefixExpression
        }

        val ConstantExpressionFilter: NodeFilter = NodeFilter {
            val element = deparIfNecessary(it)
            element is KtConstantExpression || element is KtParenthesizedExpression
        }

        val DotQualifiedExpressionFilter: NodeFilter = NodeFilter {
            val element = deparIfNecessary(it)
            element is KtDotQualifiedExpression || element is KtReferenceExpression
        }

        val ReferenceExpressionFilter: NodeFilter = NodeFilter {
            val element = deparIfNecessary(it)
            element is KtReferenceExpression
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