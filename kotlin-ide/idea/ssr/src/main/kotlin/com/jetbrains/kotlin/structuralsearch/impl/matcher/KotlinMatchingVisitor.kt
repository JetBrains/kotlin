package com.jetbrains.kotlin.structuralsearch.impl.matcher

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.structuralsearch.StructuralSearchUtil
import com.intellij.structuralsearch.impl.matcher.CompiledPattern
import com.intellij.structuralsearch.impl.matcher.GlobalMatchingVisitor
import com.intellij.structuralsearch.impl.matcher.handlers.LiteralWithSubstitutionHandler
import com.intellij.structuralsearch.impl.matcher.handlers.SubstitutionHandler
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection
import org.jetbrains.kotlin.nj2k.postProcessing.type
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.jetbrains.kotlin.psi2ir.deparenthesize
import org.jetbrains.kotlin.renderer.DescriptorRenderer

class KotlinMatchingVisitor(private val myMatchingVisitor: GlobalMatchingVisitor) : KtVisitorVoid() {
    private inline fun <reified T> getTreeElement(): T? =  when (val element = myMatchingVisitor.element) {
        is KtParenthesizedExpression -> {
            if(element is T) { // actually retrieving KtParenthesizedExpression so don't deparenthesize
                element
            } else {
                val deparenthesized = element.deparenthesize()
                if(deparenthesized is T) deparenthesized else {
                    myMatchingVisitor.result = false
                    null
                }
            }
        }
        is T -> element
        else -> {
            myMatchingVisitor.result = false
            null
        }
    }

    private fun KtExpression.countParenthesize(initial: Int = 0): Int {
        val parentheses = children.firstOrNull { it is KtParenthesizedExpression } as KtExpression?
        return parentheses?.countParenthesize(initial + 1) ?: initial
    }

    private fun GlobalMatchingVisitor.matchSequentially(elements: List<PsiElement?>, elements2: List<PsiElement?>) =
        matchSequentially(elements.toTypedArray(), elements2.toTypedArray())

    private fun GlobalMatchingVisitor.matchInAnyOrder(elements: List<PsiElement?>, elements2: List<PsiElement?>) =
        matchInAnyOrder(elements.toTypedArray(), elements2.toTypedArray())

    private fun matchTextOrVariable(el1: PsiElement?, el2: PsiElement?): Boolean {
        if (el1 == null || el2 == null) return el1 == el2
        val context = myMatchingVisitor.matchContext
        val pattern = context.pattern
        return when (val handler = pattern.getHandler(el1)) {
            is SubstitutionHandler -> handler.validate(el2, context)
            else -> myMatchingVisitor.matchText(el1, el2)
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

    override fun visitUnaryExpression(expression: KtUnaryExpression) {
        val other = getTreeElement<KtUnaryExpression>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(expression.baseExpression, other.baseExpression)
                && myMatchingVisitor.match(expression.operationReference, other.operationReference)
    }

    override fun visitParenthesizedExpression(expression: KtParenthesizedExpression) {
        val other = getTreeElement<KtParenthesizedExpression>() ?: return
        if (!myMatchingVisitor.setResult(expression.countParenthesize() == other.countParenthesize())) return
        myMatchingVisitor.result = myMatchingVisitor.match(expression.deparenthesize(), other.deparenthesize())
    }

    override fun visitConstantExpression(expression: KtConstantExpression) {
        val other = getTreeElement<KtConstantExpression>() ?: return
        myMatchingVisitor.result = expression.text == other.text
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
                myMatchingVisitor.result = myMatchingVisitor.matchText(referencedNameElement, other.getReferencedNameElement())
            }
        }
    }

    private inline fun <reified T : KtExpressionWithLabel> matchExpressionWithLabel(
        patternElement: T,
        treeElement: KtExpressionWithLabel
    ) {
        myMatchingVisitor.result = treeElement is T
                && myMatchingVisitor.match(patternElement.getTargetLabel(), treeElement.getTargetLabel())
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

    override fun visitFunctionType(type: KtFunctionType) {
        val other = getTreeElement<KtFunctionType>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(type.receiverTypeReference, other.receiverTypeReference)
                && myMatchingVisitor.matchSequentially(type.parameters, other.parameters)
    }

    override fun visitUserType(type: KtUserType) {
        val other = myMatchingVisitor.element
        myMatchingVisitor.result = when (other) {
            is KtUserType -> myMatchingVisitor.matchSonsInAnyOrder(type, other)
            is KtTypeElement -> matchTextOrVariable(type.referenceExpression, other)
            else -> false
        }
    }

    override fun visitNullableType(nullableType: KtNullableType) {
        val other = getTreeElement<KtNullableType>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.matchSons(nullableType, other)
    }

    override fun visitDynamicType(type: KtDynamicType) {
        myMatchingVisitor.result = myMatchingVisitor.element is KtDynamicType
    }

    private fun matchTypeReferenceWithDeclaration(typeReference: KtTypeReference?, other: KtDeclaration): Boolean {
        val type = other.type()
        val factory = KtPsiFactory(other, true)
        if (type != null) {
            val fqType = DescriptorRenderer.DEBUG_TEXT.renderType(type)
            return myMatchingVisitor.match(typeReference, factory.createTypeIfPossible(fqType))
        }
        return false
    }

    override fun visitTypeReference(typeReference: KtTypeReference) {
        val other = getTreeElement<KtTypeReference>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.matchSons(typeReference, other)
    }

    override fun visitQualifiedExpression(expression: KtQualifiedExpression) {
        val other = getTreeElement<KtQualifiedExpression>() ?: return
        myMatchingVisitor.result = expression.operationSign == other.operationSign
                && myMatchingVisitor.match(expression.receiverExpression, other.receiverExpression)
                && myMatchingVisitor.match(expression.selectorExpression, other.selectorExpression)
    }

    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
        val other = myMatchingVisitor.element

        // Regular matching
        if (other is KtDotQualifiedExpression) {
            visitQualifiedExpression(expression)
            return
        }

        val context = myMatchingVisitor.matchContext
        val pattern = context.pattern
        val handler = pattern.getHandler(expression.receiverExpression)

        // Match "'_?.'_"-like patterns
        myMatchingVisitor.result = handler is SubstitutionHandler
                && handler.minOccurs == 0
                && other.parent !is KtDotQualifiedExpression
                && other.parent !is KtReferenceExpression
                && myMatchingVisitor.match(expression.selectorExpression, other)
    }

    override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression) {
        val other = getTreeElement<KtLambdaExpression>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.matchInAnyOrder(lambdaExpression.valueParameters, other.valueParameters)
                && myMatchingVisitor.match(lambdaExpression.bodyExpression, other.bodyExpression)
    }

    override fun visitTypeProjection(typeProjection: KtTypeProjection) {
        val other = getTreeElement<KtTypeProjection>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(typeProjection.typeReference, other.typeReference)
    }

    override fun visitTypeArgumentList(typeArgumentList: KtTypeArgumentList) {
        val other = getTreeElement<KtTypeArgumentList>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.matchSequentially(typeArgumentList.arguments, other.arguments)
    }

    override fun visitArgument(argument: KtValueArgument) {
        val other = getTreeElement<KtValueArgument>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(argument.getArgumentExpression(), other.getArgumentExpression())
                && (!argument.isNamed() || !other.isNamed() || matchTextOrVariable(
            argument.getArgumentName(), other.getArgumentName()
        ))
    }

    private fun matchValueArgumentList(queryArgs: List<KtValueArgument>?, codeArgs: List<KtValueArgument>?): Boolean {
        if (queryArgs == null || codeArgs == null) return queryArgs == codeArgs
        var queryIndex = 0
        var codeIndex = 0
        while (queryIndex < queryArgs.size) {
            val queryArg = queryArgs[queryIndex]
            val codeArg = codeArgs.getOrElse(codeIndex) { return@matchValueArgumentList false }
            val handler = myMatchingVisitor.matchContext.pattern.getHandler(queryArg)
            if (handler is SubstitutionHandler) {
                return myMatchingVisitor.matchSequentially(
                    queryArgs.subList(queryIndex, queryArgs.lastIndex + 1),
                    codeArgs.subList(codeIndex, codeArgs.lastIndex + 1)
                )
            }

            // varargs declared in call matching with one-to-one argument passing
            if (queryArg.isSpread && !codeArg.isSpread) {
                val spreadArgExpr = queryArg.getArgumentExpression()
                if (spreadArgExpr is KtCallExpression) {
                    spreadArgExpr.valueArguments.forEach { spreadedArg ->
                        if (!myMatchingVisitor.match(spreadedArg, codeArgs[codeIndex++])) return@matchValueArgumentList false
                    }
                    queryIndex++
                    continue
                }   // can't match array that is not created in the call itself
                myMatchingVisitor.result = false
                return myMatchingVisitor.result
            }
            if (!queryArg.isSpread && codeArg.isSpread) {
                val spreadArgExpr = codeArg.getArgumentExpression()
                if (spreadArgExpr is KtCallExpression) {
                    spreadArgExpr.valueArguments.forEach { spreadedArg ->
                        if (!myMatchingVisitor.match(queryArgs[queryIndex++], spreadedArg)) return@matchValueArgumentList false
                    }
                    codeIndex++
                    continue
                }
                return false// can't match array that is not created in the call itself
            }
            // normal argument matching
            if (!myMatchingVisitor.match(queryArg, codeArg)) {
                return if (queryArg.isNamed() || codeArg.isNamed()) { // start comparing for out of order arguments
                    myMatchingVisitor.matchInAnyOrder(
                        queryArgs.subList(queryIndex, queryArgs.lastIndex + 1),
                        codeArgs.subList(codeIndex, codeArgs.lastIndex + 1)
                    )
                } else false
            }
            queryIndex++
            codeIndex++
        }
        if (codeIndex != codeArgs.size) return false
        return true
    }

    override fun visitCallExpression(expression: KtCallExpression) {
        val other = getTreeElement<KtCallExpression>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(expression.calleeExpression, other.calleeExpression)
                && myMatchingVisitor.match(expression.typeArgumentList, other.typeArgumentList)
                && matchValueArgumentList(expression.valueArguments, other.valueArguments)
    }

    override fun visitTypeParameter(parameter: KtTypeParameter) {
        val other = getTreeElement<KtTypeParameter>() ?: return
        myMatchingVisitor.result = matchTextOrVariable(parameter.firstChild, other.firstChild) // match generic identifier
                && myMatchingVisitor.match(parameter.extendsBound, other.extendsBound)
                && parameter.variance == other.variance
    }

    override fun visitParameter(parameter: KtParameter) {
        val other = getTreeElement<KtParameter>() ?: return

        val typeMatched = when (other.parent.parent) {
            is KtFunctionType, is KtCatchClause -> myMatchingVisitor.match(parameter.typeReference, other.typeReference)
            else -> matchTypeReferenceWithDeclaration(parameter.typeReference, other)
        }

        myMatchingVisitor.result = typeMatched
                && matchTextOrVariable(parameter.nameIdentifier, other.nameIdentifier)
                && myMatchingVisitor.match(parameter.defaultValue, other.defaultValue)
    }

    override fun visitTypeParameterList(list: KtTypeParameterList) {
        val other = getTreeElement<KtTypeParameterList>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.matchSequentially(list.parameters, other.parameters)
    }

    override fun visitParameterList(list: KtParameterList) {
        val other = getTreeElement<KtParameterList>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.matchSequentially(list.parameters, other.parameters)
    }

    override fun visitConstructorDelegationCall(call: KtConstructorDelegationCall) {
        val other = getTreeElement<KtConstructorDelegationCall>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(call.calleeExpression, other.calleeExpression)
                && myMatchingVisitor.match(call.typeArgumentList, other.typeArgumentList)
                && matchValueArgumentList(call.valueArgumentList?.arguments, other.valueArgumentList?.arguments)
    }

    override fun visitSecondaryConstructor(constructor: KtSecondaryConstructor) {
        val other = getTreeElement<KtSecondaryConstructor>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(constructor.modifierList, other.modifierList)
                && myMatchingVisitor.match(constructor.typeParameterList, other.typeParameterList)
                && myMatchingVisitor.match(constructor.valueParameterList, other.valueParameterList)
                && myMatchingVisitor.match(constructor.getDelegationCallOrNull(), other.getDelegationCallOrNull())
                && myMatchingVisitor.match(constructor.bodyExpression, other.bodyExpression)
    }

    override fun visitPrimaryConstructor(constructor: KtPrimaryConstructor) {
        val other = getTreeElement<KtPrimaryConstructor>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(constructor.modifierList, other.modifierList)
                && myMatchingVisitor.match(constructor.typeParameterList, other.typeParameterList)
                && myMatchingVisitor.match(constructor.valueParameterList, other.valueParameterList)
    }

    override fun visitAnonymousInitializer(initializer: KtAnonymousInitializer) {
        val other = getTreeElement<KtAnonymousInitializer>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(initializer.body, other.body)
    }

    override fun visitClassBody(classBody: KtClassBody) {
        val other = getTreeElement<KtClassBody>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.matchSonsInAnyOrder(classBody, other)
    }

    override fun visitSuperTypeListEntry(specifier: KtSuperTypeListEntry) {
        val other = getTreeElement<KtSuperTypeListEntry>() ?: return
        myMatchingVisitor.result = matchTextOrVariable(specifier, other)
    }

    override fun visitSuperTypeList(list: KtSuperTypeList) {
        val other = getTreeElement<KtSuperTypeList>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.matchSonsInAnyOrder(list, other)
    }

    override fun visitClass(klass: KtClass) {
        val other = getTreeElement<KtClass>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(klass.getClassOrInterfaceKeyword(), other.getClassOrInterfaceKeyword())
                && matchTextOrVariable(klass.nameIdentifier, other.nameIdentifier)
                && myMatchingVisitor.match(klass.modifierList, other.modifierList)
                && myMatchingVisitor.match(klass.typeParameterList, other.typeParameterList)
                && myMatchingVisitor.match(klass.primaryConstructor, other.primaryConstructor)
                && myMatchingVisitor.matchInAnyOrder(klass.secondaryConstructors, other.secondaryConstructors)
                && myMatchingVisitor.match(klass.getSuperTypeList(), other.getSuperTypeList())
                && myMatchingVisitor.match(klass.body, other.body)
                && myMatchingVisitor.match(klass.docComment, other.docComment)
        if (myMatchingVisitor.result && myMatchingVisitor.matchContext.pattern.isTypedVar(klass.nameIdentifier)) {
            myMatchingVisitor.result = myMatchingVisitor.handleTypedElement(klass.identifyingElement, other.identifyingElement)
        }
    }

    override fun visitObjectLiteralExpression(expression: KtObjectLiteralExpression) {
        val other = getTreeElement<KtObjectLiteralExpression>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(expression.objectDeclaration, other.objectDeclaration)
    }

    override fun visitObjectDeclaration(declaration: KtObjectDeclaration) {
        val other = getTreeElement<KtObjectDeclaration>() ?: return
        val otherIdentifier = if (other.isCompanion()) (other.parent.parent as KtClass).nameIdentifier else other.nameIdentifier
        myMatchingVisitor.result = matchTextOrVariable(declaration.nameIdentifier, otherIdentifier)
                && myMatchingVisitor.match(declaration.modifierList, other.modifierList)
                && myMatchingVisitor.match(declaration.getSuperTypeList(), other.getSuperTypeList())
                && myMatchingVisitor.match(declaration.body, other.body)
    }

    override fun visitNamedFunction(function: KtNamedFunction) {
        val other = getTreeElement<KtNamedFunction>() ?: return
        myMatchingVisitor.result = matchTextOrVariable(function.nameIdentifier, other.nameIdentifier)
                && myMatchingVisitor.match(function.modifierList, other.modifierList)
                && myMatchingVisitor.match(function.typeParameterList, other.typeParameterList)
                && matchTypeReferenceWithDeclaration(function.typeReference, other)
                && myMatchingVisitor.match(function.valueParameterList, other.valueParameterList)
                && myMatchingVisitor.match(function.bodyExpression, other.bodyExpression)
        if (myMatchingVisitor.result && myMatchingVisitor.matchContext.pattern.isTypedVar(function.nameIdentifier)) {
            myMatchingVisitor.result = myMatchingVisitor.handleTypedElement(function.identifyingElement, other.identifyingElement)
        }
    }

    override fun visitElement(element: PsiElement) {
        when (element) {
            is LeafPsiElement -> {
                val other = getTreeElement<LeafPsiElement>() ?: return
                myMatchingVisitor.result = element.elementType.index == other.elementType.index
            }
            is KDoc -> {
                val other = getTreeElement<KDoc>() ?: return
                myMatchingVisitor.result = myMatchingVisitor.matchSequentially(
                    element.getChildrenOfType<KDocSection>(),
                    other.getChildrenOfType<KDocSection>()
                )
            }
            is KDocSection -> {
                val other = getTreeElement<KDocSection>() ?: return
                myMatchingVisitor.result = myMatchingVisitor.matchText(element, other)
            }
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

    override fun visitForExpression(expression: KtForExpression) {
        val other = getTreeElement<KtForExpression>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(expression.loopParameter, other.loopParameter)
                && myMatchingVisitor.match(expression.loopRange, other.loopRange)
                && myMatchingVisitor.match(expression.body, other.body)
    }

    override fun visitWhileExpression(expression: KtWhileExpression) {
        val other = getTreeElement<KtWhileExpression>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(expression.condition, other.condition)
                && myMatchingVisitor.match(expression.body, other.body)
    }

    override fun visitDoWhileExpression(expression: KtDoWhileExpression) {
        val other = getTreeElement<KtDoWhileExpression>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(expression.condition, other.condition)
                && myMatchingVisitor.match(expression.body, other.body)
    }

    override fun visitWhenConditionInRange(condition: KtWhenConditionInRange) {
        val other = getTreeElement<KtWhenConditionInRange>() ?: return
        myMatchingVisitor.result = condition.isNegated == other.isNegated
                && myMatchingVisitor.match(condition.rangeExpression, other.rangeExpression)
    }

    override fun visitWhenConditionIsPattern(condition: KtWhenConditionIsPattern) {
        val other = getTreeElement<KtWhenConditionIsPattern>() ?: return
        myMatchingVisitor.result = condition.isNegated == other.isNegated
                && myMatchingVisitor.match(condition.typeReference, other.typeReference)
    }

    override fun visitWhenConditionWithExpression(condition: KtWhenConditionWithExpression) {
        val other = getTreeElement<KtWhenConditionWithExpression>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(condition.expression, other.expression)
    }

    override fun visitWhenEntry(jetWhenEntry: KtWhenEntry) {
        val other = getTreeElement<KtWhenEntry>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.matchInAnyOrder(jetWhenEntry.conditions, other.conditions)
                && myMatchingVisitor.match(jetWhenEntry.expression, other.expression)
    }

    override fun visitWhenExpression(expression: KtWhenExpression) {
        val other = getTreeElement<KtWhenExpression>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(expression.subjectExpression, other.subjectExpression)
                && myMatchingVisitor.matchInAnyOrder(expression.entries, other.entries)
    }

    override fun visitFinallySection(finallySection: KtFinallySection) {
        val other = getTreeElement<KtFinallySection>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(finallySection.finalExpression, other.finalExpression)
    }

    override fun visitCatchSection(catchClause: KtCatchClause) {
        val other = getTreeElement<KtCatchClause>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(catchClause.parameterList, other.parameterList)
                && myMatchingVisitor.match(catchClause.catchBody, other.catchBody)
    }

    override fun visitTryExpression(expression: KtTryExpression) {
        val other = getTreeElement<KtTryExpression>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(expression.tryBlock, other.tryBlock)
                && myMatchingVisitor.matchInAnyOrder(expression.catchClauses, other.catchClauses)
                && myMatchingVisitor.match(expression.finallyBlock, other.finallyBlock)
    }

    override fun visitTypeAlias(typeAlias: KtTypeAlias) {
        val other = getTreeElement<KtTypeAlias>() ?: return
        myMatchingVisitor.result = matchTextOrVariable(typeAlias.nameIdentifier, other.nameIdentifier)
                && myMatchingVisitor.match(typeAlias.getTypeReference(), other.getTypeReference())
    }

    override fun visitConstructorCalleeExpression(constructorCalleeExpression: KtConstructorCalleeExpression) {
        val other = getTreeElement<KtConstructorCalleeExpression>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(constructorCalleeExpression.typeReference, other.typeReference)
                && myMatchingVisitor.match(
            constructorCalleeExpression.constructorReferenceExpression, other.constructorReferenceExpression
        )
    }

    override fun visitAnnotationEntry(annotationEntry: KtAnnotationEntry) {
        val other = getTreeElement<KtAnnotationEntry>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(annotationEntry.calleeExpression, other.calleeExpression)
                && myMatchingVisitor.match(annotationEntry.typeArgumentList, other.typeArgumentList)
                && matchValueArgumentList(annotationEntry.valueArgumentList?.arguments, other.valueArgumentList?.arguments)
    }

    override fun visitProperty(property: KtProperty) {
        val other = getTreeElement<KtProperty>() ?: return
        myMatchingVisitor.result = matchTypeReferenceWithDeclaration(property.typeReference, other)
                && myMatchingVisitor.match(property.modifierList, other.modifierList)
                && property.isVar == other.isVar
                && matchTextOrVariable(property.nameIdentifier, other.nameIdentifier)
                && (property.delegateExpressionOrInitializer == null || myMatchingVisitor.match(
            property.delegateExpressionOrInitializer, other.delegateExpressionOrInitializer
        ))
                && myMatchingVisitor.match(property.docComment, other.docComment)
        if (myMatchingVisitor.result && myMatchingVisitor.matchContext.pattern.isTypedVar(property.nameIdentifier)) {
            myMatchingVisitor.result = myMatchingVisitor.handleTypedElement(property.identifyingElement, other.identifyingElement)
        }
    }

    override fun visitStringTemplateExpression(expression: KtStringTemplateExpression) {
        val other = getTreeElement<KtStringTemplateExpression>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.matchSequentially(expression.entries, other.entries)
    }

    override fun visitLiteralStringTemplateEntry(entry: KtLiteralStringTemplateEntry) {
        val other = myMatchingVisitor.element
        myMatchingVisitor.result = when (val handler = entry.getUserData(CompiledPattern.HANDLER_KEY)) {
            is LiteralWithSubstitutionHandler -> handler.match(entry, other, myMatchingVisitor.matchContext)
            else -> matchTextOrVariable(entry, other)
        }
    }

    override fun visitSimpleNameStringTemplateEntry(entry: KtSimpleNameStringTemplateEntry) {
        val other = myMatchingVisitor.element
        myMatchingVisitor.result = when (other) {
            is KtSimpleNameStringTemplateEntry -> matchTextOrVariable(entry.expression, other.expression)
            is KtLiteralStringTemplateEntry -> matchTextOrVariable(entry.expression, other)
            is KtEscapeStringTemplateEntry -> matchTextOrVariable(entry.expression, other)
            is KtBlockStringTemplateEntry -> myMatchingVisitor.match(entry.expression, other.expression)
            else -> false
        }
    }

    override fun visitBlockStringTemplateEntry(entry: KtBlockStringTemplateEntry) {
        val other = getTreeElement<KtBlockStringTemplateEntry>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(entry.expression, other.expression)
    }

    override fun visitEscapeStringTemplateEntry(entry: KtEscapeStringTemplateEntry) {
        val other = getTreeElement<KtEscapeStringTemplateEntry>() ?: return
        myMatchingVisitor.result = matchTextOrVariable(entry, other)
    }

    override fun visitBinaryWithTypeRHSExpression(expression: KtBinaryExpressionWithTypeRHS) {
        val other = getTreeElement<KtBinaryExpressionWithTypeRHS>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(expression.operationReference, other.operationReference)
                && myMatchingVisitor.match(expression.left, other.left)
                && myMatchingVisitor.match(expression.right, other.right)
    }

    override fun visitIsExpression(expression: KtIsExpression) {
        val other = getTreeElement<KtIsExpression>() ?: return
        myMatchingVisitor.result = expression.isNegated == other.isNegated
                && myMatchingVisitor.match(expression.leftHandSide, other.leftHandSide)
                && myMatchingVisitor.match(expression.typeReference, other.typeReference)
    }

    override fun visitDestructuringDeclaration(destructuringDeclaration: KtDestructuringDeclaration) {
        val other = getTreeElement<KtDestructuringDeclaration>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.matchSequentially(destructuringDeclaration.entries, other.entries)
                && myMatchingVisitor.match(destructuringDeclaration.initializer, other.initializer)
                && myMatchingVisitor.match(destructuringDeclaration.docComment, other.docComment)
    }

    override fun visitDestructuringDeclarationEntry(multiDeclarationEntry: KtDestructuringDeclarationEntry) {
        val other = getTreeElement<KtDestructuringDeclarationEntry>() ?: return
        myMatchingVisitor.result = matchTypeReferenceWithDeclaration(multiDeclarationEntry.typeReference, other)
                && myMatchingVisitor.match(multiDeclarationEntry.modifierList, other.modifierList)
                && multiDeclarationEntry.isVar == other.isVar
                && matchTextOrVariable(multiDeclarationEntry.nameIdentifier, other.nameIdentifier)
    }

    override fun visitComment(comment: PsiComment) {
        val other = getTreeElement<PsiComment>() ?: return

        if (!myMatchingVisitor.setResult(comment.tokenType == other.tokenType)) return

        when (val handler = comment.getUserData(CompiledPattern.HANDLER_KEY)) {
            is LiteralWithSubstitutionHandler -> {
                myMatchingVisitor.result = handler.match(comment, other, myMatchingVisitor.matchContext)
            }
            else -> {
                myMatchingVisitor.result = myMatchingVisitor.matchText(
                    StructuralSearchUtil.normalize(KotlinMatchUtil.getCommentText(comment)),
                    StructuralSearchUtil.normalize(KotlinMatchUtil.getCommentText(other))
                )
            }
        }
    }

}
