package com.jetbrains.kotlin.structuralsearch.impl.matcher

import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.structuralsearch.impl.matcher.GlobalMatchingVisitor
import com.intellij.structuralsearch.impl.matcher.handlers.SubstitutionHandler
import com.intellij.ui.SearchTextField
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.core.util.range
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.getReturnTypeReference
import org.jetbrains.kotlin.idea.refactoring.fqName.fqName
import org.jetbrains.kotlin.nj2k.postProcessing.type
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getValueParameterList
import org.jetbrains.kotlin.resolve.calls.model.isReallySuccess
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

    private fun GlobalMatchingVisitor.matchSequentially(elements: List<PsiElement>, elements2: List<PsiElement>) =
        matchSequentially(elements.toTypedArray(), elements2.toTypedArray())

    private fun GlobalMatchingVisitor.matchInAnyOrder(elements: List<PsiElement>, elements2: List<PsiElement>) =
        matchInAnyOrder(elements.toTypedArray(), elements2.toTypedArray())

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

    override fun visitTypeReference(typeReference: KtTypeReference) {
        val other = getTreeElement<KtTypeReference>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.matchSons(typeReference.typeElement, other.typeElement)
                // If typeReference or other isn't fully qualified, matches the last REFERENCE_EXPRESSION
                || (typeReference.firstChild.children.size == 1 || other.firstChild.children.size == 1)
                && myMatchingVisitor.match(typeReference.firstChild.lastChild, other.firstChild.lastChild)
    }

    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
        val other = myMatchingVisitor.element

        // Regular matching
        if (other is KtDotQualifiedExpression) {
            myMatchingVisitor.result = myMatchingVisitor.match(expression.receiverExpression, other.receiverExpression)
                    && myMatchingVisitor.match(expression.selectorExpression, other.selectorExpression)
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
        myMatchingVisitor.result = myMatchingVisitor.match(lambdaExpression.functionLiteral, other.functionLiteral)
                && myMatchingVisitor.match(lambdaExpression.bodyExpression, other.bodyExpression)
    }

    override fun visitArgument(argument: KtValueArgument) {
        val other = getTreeElement<KtValueArgument>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(argument.getArgumentName(), other.getArgumentName())
                && myMatchingVisitor.match(argument.getArgumentExpression(), other.getArgumentExpression())
    }

    override fun visitCallExpression(expression: KtCallExpression) {
        val other = getTreeElement<KtCallExpression>() ?: return

        // check callee matching
        if (!myMatchingVisitor.setResult(myMatchingVisitor.match(expression.calleeExpression, other.calleeExpression))) return

        val resolvedOther = other.resolveToCall(BodyResolveMode.PARTIAL) ?: run {
            myMatchingVisitor.result = false
            return
        }
        if (!resolvedOther.isReallySuccess()) {
            myMatchingVisitor.result = false
            return
        }

        // check type arguments matching (generics)
        val queryTypeNames = expression.typeArguments.map { it.typeReference?.typeElement?.firstChild?.firstChild?.text }
        val codeTypes = resolvedOther.typeArguments.values
        val codeTypeNames = codeTypes.map { it.toString() }
        val codeTypeFqNames = codeTypes.map { it.fqName.toString() }
        if (!myMatchingVisitor.setResult(queryTypeNames == codeTypeNames || queryTypeNames == codeTypeFqNames)) return

        // check arguments value matching
        val queryArgs = expression.valueArguments
        val sortedCodeArgs = resolvedOther.valueArgumentsByIndex?.flatMap { it.arguments }?.map { it as KtValueArgument } ?: run {
            myMatchingVisitor.result = false
            return
        }
        var queryIndex = 0
        var codeIndex = 0
        while (queryIndex < queryArgs.size) {
            val queryArg = queryArgs[queryIndex]
            val codeArg = sortedCodeArgs.getOrElse(codeIndex) {
                myMatchingVisitor.result = false
                return
            }

            // varargs declared in call matching with one-to-one argument passing
            if (queryArg.isSpread && !codeArg.isSpread) {
                val spreadArgExpr = queryArg.getArgumentExpression()
                if (spreadArgExpr is KtCallExpression) {
                    myMatchingVisitor.result = true
                    spreadArgExpr.valueArguments.forEach { spreadedArg ->
                        if (!myMatchingVisitor.setResult(myMatchingVisitor.match(spreadedArg, sortedCodeArgs[codeIndex++])))
                            return
                    }
                    queryIndex++
                    continue
                } else { // can't match array that is not created in the call itself
                    myMatchingVisitor.result = false
                    return
                }
            }
            if (!queryArg.isSpread && codeArg.isSpread) {
                val spreadArgExpr = codeArg.getArgumentExpression()
                if (spreadArgExpr is KtCallExpression) {
                    myMatchingVisitor.result = true
                    (spreadArgExpr).valueArguments.forEach { spreadedArg ->
                        if (!myMatchingVisitor.setResult(myMatchingVisitor.match(queryArgs[queryIndex++], spreadedArg))) return
                    }
                    codeIndex++
                    continue
                } else { // can't match array that is not created in the call itself
                    myMatchingVisitor.result = false
                    return
                }
            }
            // normal argument matching
            if (!myMatchingVisitor.setResult(myMatchingVisitor.match(queryArg, codeArg))) {
                if (myMatchingVisitor.setResult(queryArg.isNamed())) { // start comparing for out of order arguments
                    val sortQueryArgs = queryArgs.subList(queryIndex, expression.valueArguments.lastIndex + 1)
                        .sortedBy { it.getArgumentName()?.asName }
                    val sortCodeArgs = sortedCodeArgs.subList(codeIndex, sortedCodeArgs.lastIndex + 1)
                        .sortedBy { it.getArgumentName()?.asName }
                    myMatchingVisitor.result = myMatchingVisitor.matchSequentially(sortQueryArgs, sortCodeArgs)
                    return
                } else {
                    return // result = false
                }
            }
            queryIndex++
            codeIndex++
        }
    }

    private fun matchTextOrVariable(el1: PsiElement?, el2: PsiElement?): Boolean {
        if (el1 == null || el2 == null) return el1 == el2
        val context = myMatchingVisitor.matchContext
        val pattern = context.pattern
        return when (val handler = pattern.getHandler(el1)) {
            is SubstitutionHandler -> handler.validate(el2, context)
            else -> myMatchingVisitor.matchText(el1, el2)
        }
    }

    override fun visitTypeParameter(parameter: KtTypeParameter) {
        val other = getTreeElement<KtTypeParameter>() ?: return
        myMatchingVisitor.result = matchTextOrVariable(parameter.lastChild, other.lastChild) // match generic
                && myMatchingVisitor.match(parameter.extendsBound, other.extendsBound)
                && parameter.variance == other.variance
    }

    override fun visitParameter(parameter: KtParameter) {
        val other = getTreeElement<KtParameter>() ?: return
        myMatchingVisitor.result = matchTextOrVariable(parameter.nameIdentifier, other.nameIdentifier)
                && myMatchingVisitor.match(parameter.typeReference, other.typeReference)
                && myMatchingVisitor.match(parameter.defaultValue, other.defaultValue)
    }

    override fun visitTypeParameterList(list: KtTypeParameterList) {
        val other = getTreeElement<KtTypeParameterList>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.matchSequentially(
            list.parameters, other.parameters
        )
    }

    override fun visitParameterList(list: KtParameterList) {
        val other = getTreeElement<KtParameterList>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.matchInAnyOrder(
            list.parameters, other.parameters
        )
    }

    override fun visitConstructorDelegationCall(call: KtConstructorDelegationCall) {
        val other = getTreeElement<KtConstructorDelegationCall>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(call.calleeExpression, other.calleeExpression)
                && myMatchingVisitor.match(call.typeArgumentList, other.typeArgumentList)
                && myMatchingVisitor.match(call.valueArgumentList, other.valueArgumentList)
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
    }

    override fun visitNamedFunction(function: KtNamedFunction) {
        val other = getTreeElement<KtNamedFunction>() ?: return
        myMatchingVisitor.result = matchTextOrVariable(function.nameIdentifier, other.nameIdentifier)
                && myMatchingVisitor.match(function.modifierList, other.modifierList)
                && myMatchingVisitor.match(function.typeReference, other.typeReference)
                && myMatchingVisitor.match(function.typeParameterList, other.typeParameterList)
                && myMatchingVisitor.match(function.valueParameterList, other.valueParameterList)
                && myMatchingVisitor.match(function.bodyExpression, other.bodyExpression)
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

    override fun visitProperty(property: KtProperty) {
        val other = getTreeElement<KtProperty>() ?: return
        val patternTypeReference = property.typeReference
        val codeType = other.type()

        val typeMatched = when {
            // type() function returns [KotlinType?]
            codeType == null -> patternTypeReference == null
            // Type will be matched with the delegateExpressionOrInitializer matching
            patternTypeReference == null -> true
            // Short typeReference name
            myMatchingVisitor.matchText(patternTypeReference.text, codeType.toString()) -> true
            // FQ typeReference name
            myMatchingVisitor.matchText(patternTypeReference.text, codeType.fqName.toString()) -> true
            else -> false
        }

        myMatchingVisitor.result = typeMatched
                && myMatchingVisitor.match(property.modifierList, other.modifierList)
                && property.isVar == other.isVar
                && matchTextOrVariable(property.nameIdentifier, other.nameIdentifier)
                && (property.delegateExpressionOrInitializer == null || myMatchingVisitor.match(
            property.delegateExpressionOrInitializer, other.delegateExpressionOrInitializer
        ))
    }



    override fun visitStringTemplateExpression(expression: KtStringTemplateExpression) {
        val other = getTreeElement<KtStringTemplateExpression>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.matchSequentially(expression.entries, other.entries)
    }

    override fun visitLiteralStringTemplateEntry(entry: KtLiteralStringTemplateEntry) {
        myMatchingVisitor.result = matchTextOrVariable(entry, myMatchingVisitor.element)
    }

    override fun visitSimpleNameStringTemplateEntry(entry: KtSimpleNameStringTemplateEntry) {
        val other = getTreeElement<KtSimpleNameStringTemplateEntry>() ?: return
        myMatchingVisitor.result = matchTextOrVariable(entry.expression, other.expression)
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
}