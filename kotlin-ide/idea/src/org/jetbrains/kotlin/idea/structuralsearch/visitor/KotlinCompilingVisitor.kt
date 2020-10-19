package org.jetbrains.kotlin.idea.structuralsearch.visitor

import com.intellij.dupLocator.util.NodeFilter
import com.intellij.openapi.util.Key
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
import org.jetbrains.kotlin.idea.structuralsearch.getCommentText
import org.jetbrains.kotlin.idea.structuralsearch.handler.CommentedDeclarationHandler
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.idea.structuralsearch.withinHierarchyTextFilterSet
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.kdoc.psi.impl.KDocLink
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi2ir.deparenthesize
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
        super.visitElement(element)
    }

    override fun visitReferenceExpression(expression: KtReferenceExpression) {
        visitElement(expression)
        val handler = getHandler(expression)
        getHandler(expression).filter =
            if (handler is SubstitutionHandler) NodeFilter { it is PsiElement } // accept all
            else ReferenceExpressionFilter
    }

    override fun visitLeafPsiElement(leafPsiElement: LeafPsiElement) {
        getHandler(leafPsiElement).setFilter { it is LeafPsiElement }

        when (leafPsiElement.elementType) {
            KDocTokens.TEXT -> processPatternStringWithFragments(leafPsiElement)
            KDocTokens.TAG_NAME -> {
                val handler = getHandler(leafPsiElement)
                if (handler is SubstitutionHandler) {
                    handler.findRegExpPredicate()?.setNodeTextGenerator { it.text.drop(1) }
                    if (handler.minOccurs != 1 || handler.maxOccurs != 1) {
                        setHandler(
                            leafPsiElement.parent, SubstitutionHandler(
                                "${handler.name}_",
                                false,
                                handler.minOccurs,
                                handler.maxOccurs,
                                false
                            ).apply {
                                filter = NodeFilter { it is KDocTag }
                            }
                        )
                        leafPsiElement.resetCountFilter()
                    }
                }
            }
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

    override fun visitNamedDeclaration(declaration: KtNamedDeclaration) {
        super.visitNamedDeclaration(declaration)

        declaration.nameIdentifier?.let { identifier ->
            if (getHandler(identifier).withinHierarchyTextFilterSet && declaration.parent is KtClassBody) {
                val klass = declaration.parent.parent
                if (klass is KtClassOrObject) {
                    klass.nameIdentifier?.putUserData(WITHIN_HIERARCHY, true)
                }
            }
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

    override fun visitTypeProjection(typeProjection: KtTypeProjection) {
        super.visitTypeProjection(typeProjection)
        val handler = getHandler(typeProjection)
        if (handler is SubstitutionHandler) {
            typeProjection.typeReference?.resetCountFilter()
            typeProjection.typeReference?.typeElement?.resetCountFilter()
            typeProjection.typeReference?.typeElement?.firstChild?.resetCountFilter()
            typeProjection.typeReference?.typeElement?.firstChild?.firstChild?.resetCountFilter()
        }
    }

    override fun visitModifierList(list: KtModifierList) {
        super.visitModifierList(list)
        list.setAbsenceOfMatchHandlerIfApplicable(true)
    }

    override fun visitParameterList(list: KtParameterList) {
        super.visitParameterList(list)
        list.setAbsenceOfMatchHandlerIfApplicable()
    }

    override fun visitValueArgumentList(list: KtValueArgumentList) {
        super.visitValueArgumentList(list)
        list.setAbsenceOfMatchHandlerIfApplicable()
    }

    override fun visitTypeParameterList(list: KtTypeParameterList) {
        super.visitTypeParameterList(list)
        list.setAbsenceOfMatchHandlerIfApplicable()
    }

    override fun visitTypeArgumentList(typeArgumentList: KtTypeArgumentList) {
        super.visitTypeArgumentList(typeArgumentList)
        typeArgumentList.setAbsenceOfMatchHandlerIfApplicable()
    }

    override fun visitClassBody(classBody: KtClassBody) {
        super.visitClassBody(classBody)
        classBody.setAbsenceOfMatchHandlerIfApplicable()
    }

    override fun visitPrimaryConstructor(constructor: KtPrimaryConstructor) {
        super.visitPrimaryConstructor(constructor)
        constructor.setAbsenceOfMatchHandlerIfApplicable()
    }

    override fun visitWhenEntry(jetWhenEntry: KtWhenEntry) {
        super.visitWhenEntry(jetWhenEntry)
        val condition = jetWhenEntry.firstChild.firstChild
        if (condition is KtNameReferenceExpression) {
            val handler = getHandler(condition)
            if (handler !is SubstitutionHandler) return

            setHandler(jetWhenEntry, SubstitutionHandler(handler.name, false, handler.minOccurs, handler.maxOccurs, false))
            condition.parent.resetCountFilter()
            condition.resetCountFilter()
            condition.firstChild.resetCountFilter()
        }
    }

    override fun visitConstructorCalleeExpression(expression: KtConstructorCalleeExpression) {
        super.visitConstructorCalleeExpression(expression)
        val handler = getHandler(expression)
        if (handler is SubstitutionHandler && handler.minOccurs == 0) {
            setHandler(expression.parent, SubstitutionHandler("${expression.parent.hashCode()}_optional", false, 0, handler.maxOccurs, false))
            expression.parent.parent.setAbsenceOfMatchHandlerIfApplicable()
        }
    }

//    override fun visitSuperTypeEntry(specifier: KtSuperTypeEntry) {
//        super.visitSuperTypeEntry(specifier)
//        if (specifier.allowsAbsenceOfMatch) {
//            specifier.parent.setAbsenceOfMatchHandler()
//            specifier.parent.parent.setAbsenceOfMatchHandlerIfApplicable()
//        }
//    }

    override fun visitKDoc(kDoc: KDoc) {
        getHandler(kDoc).setFilter { it is KDoc }
    }

    override fun visitKDocLink(link: KDocLink) {
        getHandler(link).setFilter { it is KDocLink }
    }

    override fun visitKDocTag(tag: KDocTag) {
        getHandler(tag).setFilter { it is KDocTag }
    }

    private fun PsiElement.setAbsenceOfMatchHandler() {
        setHandler(this, absenceOfMatchHandler(this))
    }

    private fun PsiElement.setAbsenceOfMatchHandlerIfApplicable(considerAllChildren: Boolean = false) {
        val childrenAllowAbsenceOfMatch =
            if (considerAllChildren) this.allChildren.all { it.allowsAbsenceOfMatch }
            else this.children.all { it.allowsAbsenceOfMatch }
        if (childrenAllowAbsenceOfMatch)
            setAbsenceOfMatchHandler()
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
            val newHandler = SubstitutionHandler(handler.name, handler.isTarget, 1, 1, false)
            val predicate = handler.predicate
            if (predicate != null) newHandler.predicate = predicate
            setHandler(this, newHandler)
        }
    }

    companion object {

        val WITHIN_HIERARCHY: Key<Boolean> = Key<Boolean>("withinHierarchy")

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