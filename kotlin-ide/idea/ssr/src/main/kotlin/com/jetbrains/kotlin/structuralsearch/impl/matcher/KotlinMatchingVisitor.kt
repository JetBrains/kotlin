package com.jetbrains.kotlin.structuralsearch.impl.matcher

import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.structuralsearch.impl.matcher.GlobalMatchingVisitor
import com.intellij.structuralsearch.impl.matcher.handlers.SubstitutionHandler
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.refactoring.fqName.fqName
import org.jetbrains.kotlin.nj2k.postProcessing.type
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class KotlinMatchingVisitor(private val myMatchingVisitor: GlobalMatchingVisitor) : KtVisitorVoid() {

    /**
     * Casts the current code element to [T], sets [myMatchingVisitor].result to false else.
     */
    private inline fun <reified T> getTreeElement(): T? = when (myMatchingVisitor.element) {
        is T -> myMatchingVisitor.element as T
        else -> {
            myMatchingVisitor.result = false
            null
        }
    }

    override fun visitArrayAccessExpression(expression: KtArrayAccessExpression) {
        val other = getTreeElement<KtArrayAccessExpression>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(expression.arrayExpression, other.arrayExpression)
                && myMatchingVisitor.matchSons(expression.indicesNode, other.indicesNode)
    }

    override fun visitBinaryExpression(expression: KtBinaryExpression) {
        val other = getTreeElement<KtBinaryExpression>() ?: return
        myMatchingVisitor.result = expression.operationToken == other.operationToken
                && myMatchingVisitor.match(expression.left, other.left)
                && myMatchingVisitor.match(expression.right, other.right)
    }

    override fun visitBlockExpression(expression: KtBlockExpression) {
        val other = getTreeElement<KtBlockExpression>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.matchSons(expression, other)
    }

    override fun visitPrefixExpression(expression: KtPrefixExpression) {
        val other = getTreeElement<KtPrefixExpression>() ?: return
        myMatchingVisitor.result = expression.operationToken == other.operationToken
                && myMatchingVisitor.match(expression.lastChild, other.lastChild) // check operand
    }

    override fun visitPostfixExpression(expression: KtPostfixExpression) {
        val other = getTreeElement<KtPostfixExpression>() ?: return
        myMatchingVisitor.result = expression.operationToken == other.operationToken
                && myMatchingVisitor.match(expression.firstChild, other.firstChild) // check operand
    }

    override fun visitConstantExpression(expression: KtConstantExpression) {
        myMatchingVisitor.result = myMatchingVisitor.element.text == expression.text
    }

    override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
        val context = myMatchingVisitor.matchContext
        val pattern = context.pattern
        val referencedNameElement = expression.getReferencedNameElement()

        var handler = pattern.getHandlerSimple(referencedNameElement)
        if (handler == null) {
            handler = pattern.getHandlerSimple(expression)
        }

        val other = myMatchingVisitor.element
        if (handler is SubstitutionHandler) {
            val validated = when (other) {
                // For labels, get rid of the starting '@'
                is KtSimpleNameExpression -> handler.validate(other.getReferencedNameElement(), context)
                else -> handler.validate(other, context)
            }
            if (myMatchingVisitor.setResult(validated)) {
                handler.addResult(other, context)
            }
        } else {
            if (other is KtSimpleNameExpression) {
                myMatchingVisitor.result =
                    myMatchingVisitor.matchText(referencedNameElement, other.getReferencedNameElement())
            }
        }
    }

    private inline fun <reified T : KtExpressionWithLabel> matchExpressionWithLabel(
        patternElement: T,
        treeElement: KtExpressionWithLabel
    ) {
        myMatchingVisitor.result =
            treeElement is T && myMatchingVisitor.match(patternElement.getTargetLabel(), treeElement.getTargetLabel())
    }

    override fun visitExpressionWithLabel(expression: KtExpressionWithLabel) {
        val other = getTreeElement<KtExpressionWithLabel>() ?: return
        when (expression) {
            is KtBreakExpression -> matchExpressionWithLabel(expression, other)
            is KtContinueExpression -> matchExpressionWithLabel(expression, other)
            is KtThisExpression -> matchExpressionWithLabel(expression, other)
            is KtSuperExpression -> {
                myMatchingVisitor.result = other is KtSuperExpression
                        && myMatchingVisitor.match(expression.getTargetLabel(), other.getTargetLabel())
                        && myMatchingVisitor.match(expression.superTypeQualifier, other.superTypeQualifier)
            }
            is KtReturnExpression -> {
                myMatchingVisitor.result = other is KtReturnExpression
                        && myMatchingVisitor.match(expression.getTargetLabel(), other.getTargetLabel())
                        && myMatchingVisitor.match(expression.returnedExpression, other.returnedExpression)
            }
        }
    }

    override fun visitTypeReference(typeReference: KtTypeReference) {
        val other = getTreeElement<KtTypeReference>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.matchSons(typeReference.typeElement, other.typeElement)
    }

    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
        val other = getTreeElement<KtDotQualifiedExpression>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(expression.receiverExpression, other.receiverExpression)
                && myMatchingVisitor.match(expression.selectorExpression, other.selectorExpression)
    }

    override fun visitArgument(argument: KtValueArgument) {
        val other = getTreeElement<KtValueArgument>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.matchSons(argument, other)
    }

    override fun visitCallExpression(expression: KtCallExpression) {
        val other = getTreeElement<KtCallExpression>() ?: return
        val sortedCodeArgs = other.resolveToCall(BodyResolveMode.PARTIAL)?.valueArgumentsByIndex ?: error(
            "Could not resolve matching procedure declaration."
        )
        myMatchingVisitor.result = myMatchingVisitor.match(expression.calleeExpression, other.calleeExpression)
        sortedCodeArgs.forEachIndexed { i, codeResArg ->
            val queryValueArgs = expression.valueArguments
            val queryValueArg = queryValueArgs[i]
            val codeValueArg = codeResArg.arguments.first() as KtValueArgument
            if (!myMatchingVisitor.setResult(
                    myMatchingVisitor.match(
                        queryValueArg.getArgumentExpression(), codeValueArg.getArgumentExpression()
                    )
                ) && queryValueArg.isNamed()
            ) { // found out of order argument that could be correct
                val queryValueArgMap = queryValueArgs.subList(i, expression.valueArguments.lastIndex + 1)
                    .sortedBy { it.getArgumentName()?.asName }.map { it.getArgumentExpression() }
                val codeValueArgMap = sortedCodeArgs.subList(i, sortedCodeArgs.lastIndex + 1)
                    .map { it.arguments.first() }.sortedBy { it.getArgumentName()?.asName }.map { it.getArgumentExpression() }
                queryValueArgMap.forEachIndexed { j, queryExpr ->
                    val codeExpr = codeValueArgMap[j]
                    if (!myMatchingVisitor.setResult(myMatchingVisitor.match(queryExpr, codeExpr))) return
                }
                return
            } else { // match violation found
                return
            }
        }
    }

    private fun matchNameIdentifiers(el1: PsiElement?, el2: PsiElement?): Boolean {
        if (el1 == null || el2 == null) return el1 == el2
        val context = myMatchingVisitor.matchContext
        val pattern = context.pattern
        return when (val handler = pattern.getHandler(el1)) {
            is SubstitutionHandler -> handler.validate(el2, context)
            else -> myMatchingVisitor.matchText(el1, el2)
        }
    }

    override fun visitClass(klass: KtClass) {
        val other = getTreeElement<KtClass>() ?: return
        myMatchingVisitor.result = matchNameIdentifiers(klass.nameIdentifier, other.nameIdentifier)
                && myMatchingVisitor.match(klass.getClassKeyword(), other.getClassKeyword())
                && myMatchingVisitor.match(klass.modifierList, other.modifierList)
                && myMatchingVisitor.matchSons(klass.body, other.body)
    }

    override fun visitElement(element: PsiElement) {
        if (element is LeafPsiElement) {
            val other = getTreeElement<LeafPsiElement>() ?: return
            myMatchingVisitor.result = element.elementType.index == other.elementType.index
        }
    }

    override fun visitModifierList(list: KtModifierList) {
        val other = getTreeElement<KtModifierList>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.matchSons(list, other)
    }

    override fun visitIfExpression(expression: KtIfExpression) {
        val other = getTreeElement<KtIfExpression>() ?: return
        val elseBranch = expression.`else`
        myMatchingVisitor.result = myMatchingVisitor.match(expression.condition, other.condition)
                && myMatchingVisitor.match(expression.then, other.then)
                && (elseBranch == null || myMatchingVisitor.match(elseBranch, other.`else`))
    }

    override fun visitProperty(property: KtProperty) {
        val other = getTreeElement<KtProperty>() ?: return

        val typeMatched = when (val propertyTR = property.typeReference) {
            null -> true // Type will be matched with delegateExpressionOrInitializer
            else -> propertyTR.text == other.type().toString()
                    || propertyTR.text == other.type()?.fqName.toString()
        }

        myMatchingVisitor.result = typeMatched
                && myMatchingVisitor.match(property.modifierList, other.modifierList)
                && property.isVar == other.isVar
                && matchNameIdentifiers(property.nameIdentifier, other.nameIdentifier)
                && (property.delegateExpressionOrInitializer == null || myMatchingVisitor.match(
            property.delegateExpressionOrInitializer,
            other.delegateExpressionOrInitializer
        ))
    }
}