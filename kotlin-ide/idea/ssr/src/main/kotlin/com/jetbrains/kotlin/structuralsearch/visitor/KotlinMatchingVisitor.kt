package com.jetbrains.kotlin.structuralsearch.visitor

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.structuralsearch.StructuralSearchUtil
import com.intellij.structuralsearch.impl.matcher.CompiledPattern
import com.intellij.structuralsearch.impl.matcher.GlobalMatchingVisitor
import com.intellij.structuralsearch.impl.matcher.handlers.LiteralWithSubstitutionHandler
import com.intellij.structuralsearch.impl.matcher.handlers.SubstitutionHandler
import com.jetbrains.kotlin.structuralsearch.binaryExprOpName
import com.jetbrains.kotlin.structuralsearch.getCommentText
import org.jetbrains.kotlin.fir.builder.toUnaryName
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.idea.intentions.calleeName
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.kdoc.psi.impl.KDocLink
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.nj2k.postProcessing.type
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.jetbrains.kotlin.psi2ir.deparenthesize
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.util.OperatorNameConventions

class KotlinMatchingVisitor(private val myMatchingVisitor: GlobalMatchingVisitor) : KtVisitorVoid() {
    /** Gets the next element in the query tree and removes unnecessary parentheses. */
    private inline fun <reified T> getTreeElementDepar(): T? = when (val element = myMatchingVisitor.element) {
        is KtParenthesizedExpression -> {
            val deparenthesized = element.deparenthesize()
            if (deparenthesized is T) deparenthesized else {
                myMatchingVisitor.result = false
                null
            }
        }
        else -> getTreeElement<T>()
    }

    /** Gets the next element in the tree */
    private inline fun <reified T> getTreeElement(): T? = when (val element = myMatchingVisitor.element) {
        is T -> element
        else -> {
            myMatchingVisitor.result = false
            null
        }
    }

    private fun GlobalMatchingVisitor.matchSequentially(elements: List<PsiElement?>, elements2: List<PsiElement?>) =
        matchSequentially(elements.toTypedArray(), elements2.toTypedArray())

    private fun GlobalMatchingVisitor.matchInAnyOrder(elements: List<PsiElement?>, elements2: List<PsiElement?>) =
        matchInAnyOrder(elements.toTypedArray(), elements2.toTypedArray())

    private fun getHandler(element: PsiElement) = myMatchingVisitor.matchContext.pattern.getHandler(element)

    private fun matchTextOrVariable(el1: PsiElement?, el2: PsiElement?): Boolean {
        if (el1 == null || el2 == null) return el1 == el2
        return when (val handler = getHandler(el1)) {
            is SubstitutionHandler -> handler.handle(el2, myMatchingVisitor.matchContext)
            else -> myMatchingVisitor.matchText(el1, el2)
        }
    }

    override fun visitElement(element: PsiElement) {
        when (element) {
            is LeafPsiElement -> visitLeafPsiElement(element)
            is KDoc -> visitKDoc(element)
            is KDocSection -> visitKDocSection(element)
            is KDocTag -> visitKDocTag(element)
            is KDocLink -> visitKDocLink(element)
        }
    }

    private fun visitLeafPsiElement(element: LeafPsiElement) {
        val other = getTreeElementDepar<LeafPsiElement>() ?: return

        // Match element type
        if (!myMatchingVisitor.setResult(element.elementType == other.elementType)) return

        when (element.elementType) {
            KDocTokens.TEXT, KDocTokens.TAG_NAME -> {
                myMatchingVisitor.result = when (val handler = element.getUserData(CompiledPattern.HANDLER_KEY)) {
                    is LiteralWithSubstitutionHandler -> handler.match(element, other, myMatchingVisitor.matchContext)
                    else -> matchTextOrVariable(element, other)
                }
            }
        }
    }

    override fun visitArrayAccessExpression(expression: KtArrayAccessExpression) {
        val other = getTreeElementDepar<KtExpression>() ?: return
        myMatchingVisitor.result = when (other) {
            is KtArrayAccessExpression -> myMatchingVisitor.match(expression.arrayExpression, other.arrayExpression)
                    && myMatchingVisitor.matchSons(expression.indicesNode, other.indicesNode)
            is KtDotQualifiedExpression -> myMatchingVisitor.match(expression.arrayExpression, other.receiverExpression)
                    && other.calleeName == "${OperatorNameConventions.GET}"
                    && myMatchingVisitor.matchSequentially(
                expression.indexExpressions, other.callExpression?.valueArguments?.map(KtValueArgument::getArgumentExpression)!!
            )
            else -> false
        }

    }

    /** Matches binary expressions including translated operators. */
    override fun visitBinaryExpression(expression: KtBinaryExpression) {
        fun KtBinaryExpression.match(other: KtBinaryExpression) = operationToken == other.operationToken
                && myMatchingVisitor.match(left, other.left)
                && myMatchingVisitor.match(right, other.right)

        fun KtQualifiedExpression.match(name: Name?, receiver: KtExpression?, callEntry: KtExpression?): Boolean {
            val callExpr = callExpression
            return callExpr is KtCallExpression && calleeName == "$name"
                    && myMatchingVisitor.match(receiver, receiverExpression)
                    && myMatchingVisitor.match(callEntry, callExpr.valueArguments.first().getArgumentExpression())
        }

        fun KtBinaryExpression.matchEq(other: KtBinaryExpression): Boolean {
            val otherLeft = other.left?.deparenthesize()
            val otherRight = other.right?.deparenthesize()
            return otherLeft is KtSafeQualifiedExpression
                    && otherLeft.match(OperatorNameConventions.EQUALS, left, right)
                    && other.operationToken == KtTokens.ELVIS
                    && otherRight is KtBinaryExpression
                    && myMatchingVisitor.match(right, otherRight.left)
                    && otherRight.operationToken == KtTokens.EQEQEQ
                    && myMatchingVisitor.match(KtPsiFactory(other, true).createExpression("null"), otherRight.right)
        }

        val other = getTreeElementDepar<KtExpression>() ?: return
        when (other) {
            is KtBinaryExpression -> {
                if (myMatchingVisitor.setResult(expression.match(other))) return
                when (expression.operationToken) { // translated matching
                    KtTokens.GT, KtTokens.LT, KtTokens.GTEQ, KtTokens.LTEQ -> { // a.compareTo(b) OP 0
                        val left = other.left?.deparenthesize()
                        myMatchingVisitor.result = left is KtDotQualifiedExpression
                                && left.match(OperatorNameConventions.COMPARE_TO, expression.left, expression.right)
                                && expression.operationToken == other.operationToken
                                && myMatchingVisitor.match(other.right, KtPsiFactory(other, true).createExpression("0"))

                    }
                    KtTokens.EQEQ -> { // match a?.equals(b) ?: (b === null)
                        myMatchingVisitor.result = expression.matchEq(other)
                    }
                }
            }
            is KtDotQualifiedExpression -> { // translated matching
                val token = expression.operationToken
                val left = expression.left
                val right = expression.right
                when {
                    token == KtTokens.IN_KEYWORD -> { // b.contains(a)
                        val parent = other.parent
                        val isNotNegated = if (parent is KtPrefixExpression) parent.operationToken != KtTokens.EXCL else true
                        myMatchingVisitor.result = isNotNegated && other.match(OperatorNameConventions.CONTAINS, right, left)
                    }
                    token == KtTokens.NOT_IN -> myMatchingVisitor.result = false // already matches with prefix expression
                    token == KtTokens.EQ && left is KtArrayAccessExpression -> { // a[x] = expression
                        val matchedArgs = left.indexExpressions.apply { add(right) }
                        myMatchingVisitor.result = myMatchingVisitor.match(left.arrayExpression, other.receiverExpression)
                                && other.calleeName == "${OperatorNameConventions.SET}"
                                && myMatchingVisitor.matchSequentially(
                            matchedArgs, other.callExpression?.valueArguments?.map(KtValueArgument::getArgumentExpression)!!
                        )
                    }
                    else -> { // a.plus(b) all arithmetic operators
                        val selector = other.selectorExpression
                        myMatchingVisitor.result = selector is KtCallExpression && other.match(
                            expression.operationToken.binaryExprOpName(), left, right
                        )
                    }
                }
            }
            is KtPrefixExpression -> { // translated matching
                val baseExpr = other.baseExpression?.deparenthesize()
                when (expression.operationToken) {
                    KtTokens.NOT_IN -> { // !b.contains(a)
                        myMatchingVisitor.result = other.operationToken == KtTokens.EXCL
                                && baseExpr is KtDotQualifiedExpression
                                && baseExpr.match(OperatorNameConventions.CONTAINS, expression.right, expression.left)
                    }
                    KtTokens.EXCLEQ -> { // !(a?.equals(b) ?: (b === null))
                        myMatchingVisitor.result = other.operationToken == KtTokens.EXCL
                                && baseExpr is KtBinaryExpression
                                && expression.matchEq(baseExpr)
                    }
                }
            }
            else -> myMatchingVisitor.result = false
        }
    }

    override fun visitBlockExpression(expression: KtBlockExpression) {
        val other = getTreeElementDepar<KtBlockExpression>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.matchSons(expression, other)
    }

    override fun visitUnaryExpression(expression: KtUnaryExpression) {
        val other = getTreeElementDepar<KtExpression>() ?: return
        myMatchingVisitor.result = when (other) {
            is KtDotQualifiedExpression -> {
                myMatchingVisitor.match(expression.baseExpression, other.receiverExpression)
                        && expression.operationToken.toUnaryName().toString() == other.calleeName
            }
            is KtUnaryExpression -> myMatchingVisitor.match(expression.baseExpression, other.baseExpression)
                    && myMatchingVisitor.match(expression.operationReference, other.operationReference)
            else -> false
        }

    }

    override fun visitParenthesizedExpression(expression: KtParenthesizedExpression) {
        fun KtExpression.countParenthesize(initial: Int = 0): Int {
            val parentheses = children.firstOrNull { it is KtParenthesizedExpression } as KtExpression?
            return parentheses?.countParenthesize(initial + 1) ?: initial
        }

        val other = getTreeElement<KtParenthesizedExpression>() ?: return
        if (!myMatchingVisitor.setResult(expression.countParenthesize() == other.countParenthesize())) return
        myMatchingVisitor.result = myMatchingVisitor.match(expression.deparenthesize(), other.deparenthesize())
    }

    override fun visitConstantExpression(expression: KtConstantExpression) {
        val other = getTreeElementDepar<KtConstantExpression>() ?: return
        myMatchingVisitor.result = matchTextOrVariable(expression, other)
    }

    override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
        // Previous SubstitutionHandler match can be KtParameter
        if (myMatchingVisitor.element.parent is KtParameter) {
            myMatchingVisitor.result = matchTextOrVariable(expression, myMatchingVisitor.element)
            return
        }

        val other = getTreeElementDepar<KtElement>() ?: return
        val handler = getHandler(expression)
        myMatchingVisitor.result = when (handler) {
            is SubstitutionHandler -> handler.handle(other, myMatchingVisitor.matchContext)
            else -> matchTextOrVariable(
                expression.getReferencedNameElement(),
                if (other is KtSimpleNameExpression) other.getReferencedNameElement() else other
            )
        }
    }

    override fun visitContinueExpression(expression: KtContinueExpression) {
        val other = getTreeElementDepar<KtContinueExpression>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(expression.getTargetLabel(), other.getTargetLabel())
    }

    override fun visitBreakExpression(expression: KtBreakExpression) {
        val other = getTreeElementDepar<KtBreakExpression>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(expression.getTargetLabel(), other.getTargetLabel())
    }

    override fun visitThisExpression(expression: KtThisExpression) {
        val other = getTreeElementDepar<KtThisExpression>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(expression.getTargetLabel(), other.getTargetLabel())
    }

    override fun visitSuperExpression(expression: KtSuperExpression) {
        val other = getTreeElementDepar<KtSuperExpression>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(expression.getTargetLabel(), other.getTargetLabel())
                && myMatchingVisitor.match(expression.superTypeQualifier, other.superTypeQualifier)
    }

    override fun visitReturnExpression(expression: KtReturnExpression) {
        val other = getTreeElementDepar<KtReturnExpression>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(expression.getTargetLabel(), other.getTargetLabel())
                && myMatchingVisitor.match(expression.returnedExpression, other.returnedExpression)
    }

    override fun visitFunctionType(type: KtFunctionType) {
        val other = getTreeElementDepar<KtFunctionType>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(type.receiverTypeReference, other.receiverTypeReference)
                && myMatchingVisitor.match(type.parameterList, other.parameterList)
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
        val other = getTreeElementDepar<KtNullableType>() ?: return
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
        // Previous SubstitutionHandler match can be KtTypeParameter
        if (myMatchingVisitor.element is LeafPsiElement && myMatchingVisitor.element.parent is KtTypeParameter) {
            myMatchingVisitor.result = matchTextOrVariable(typeReference, myMatchingVisitor.element)
            return
        }

        val other = getTreeElementDepar<KtTypeReference>() ?: return
        val handler = getHandler(typeReference)
        if (handler is SubstitutionHandler) {
            myMatchingVisitor.result = handler.handle(other, myMatchingVisitor.matchContext)
            return
        }
        myMatchingVisitor.result = myMatchingVisitor.matchSons(typeReference, other)
    }

    override fun visitQualifiedExpression(expression: KtQualifiedExpression) {
        val other = getTreeElementDepar<KtQualifiedExpression>() ?: return
        myMatchingVisitor.result = expression.operationSign == other.operationSign
                && myMatchingVisitor.match(expression.receiverExpression, other.receiverExpression)
                && myMatchingVisitor.match(expression.selectorExpression, other.selectorExpression)
    }

    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
        val other = getTreeElementDepar<KtExpression>() ?: return
        if (other is KtDotQualifiedExpression) { // Regular matching
            myMatchingVisitor.result = myMatchingVisitor.match(expression.receiverExpression, other.receiverExpression)
                    && myMatchingVisitor.match(expression.selectorExpression, other.selectorExpression)
        } else { // Match '_?.'_
            val handler = getHandler(expression.receiverExpression)
            myMatchingVisitor.result = handler is SubstitutionHandler
                    && handler.minOccurs == 0
                    && other.parent !is KtDotQualifiedExpression
                    && other.parent !is KtReferenceExpression
                    && myMatchingVisitor.match(expression.selectorExpression, other)
        }
    }

    override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression) {
        val other = getTreeElementDepar<KtLambdaExpression>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.matchInAnyOrder(lambdaExpression.valueParameters, other.valueParameters)
                && myMatchingVisitor.match(lambdaExpression.bodyExpression, other.bodyExpression)
    }

    override fun visitTypeProjection(typeProjection: KtTypeProjection) {
        val other = getTreeElementDepar<KtTypeProjection>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(typeProjection.typeReference, other.typeReference)
    }

    override fun visitTypeArgumentList(typeArgumentList: KtTypeArgumentList) {
        val other = getTreeElementDepar<KtTypeArgumentList>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.matchSequentially(typeArgumentList.arguments, other.arguments)
    }

    override fun visitArgument(argument: KtValueArgument) {
        val other = getTreeElementDepar<KtValueArgument>() ?: return
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
            if (getHandler(queryArg) is SubstitutionHandler) {
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
        val other = getTreeElementDepar<KtExpression>() ?: return
        myMatchingVisitor.result = when (other) {
            is KtCallExpression -> myMatchingVisitor.match(expression.calleeExpression, other.calleeExpression)
                    && myMatchingVisitor.match(expression.typeArgumentList, other.typeArgumentList)
                    && matchValueArgumentList(expression.valueArguments, other.valueArguments)
            is KtDotQualifiedExpression -> myMatchingVisitor.match(expression.calleeExpression, other.receiverExpression)
                    && other.calleeName == "${OperatorNameConventions.INVOKE}"
                    && matchValueArgumentList(expression.valueArguments, other.callExpression?.valueArguments)
            else -> false
        }
    }

    override fun visitCallableReferenceExpression(expression: KtCallableReferenceExpression) {
        val other = getTreeElementDepar<KtCallableReferenceExpression>() ?: return
        myMatchingVisitor.match(expression.callableReference, other.callableReference)
                && myMatchingVisitor.match(expression.receiverExpression, other.receiverExpression)
    }

    override fun visitTypeParameter(parameter: KtTypeParameter) {
        val other = getTreeElementDepar<KtTypeParameter>() ?: return
        myMatchingVisitor.result = matchTextOrVariable(parameter.firstChild, other.firstChild) // match generic identifier
                && myMatchingVisitor.match(parameter.extendsBound, other.extendsBound)
                && parameter.variance == other.variance
    }

    override fun visitParameter(parameter: KtParameter) {
        // Previous SubstitutionHandler match can be KtTypeParameter
        if (myMatchingVisitor.element.parent is KtTypeParameter) {
            myMatchingVisitor.result = matchTextOrVariable(parameter, myMatchingVisitor.element)
            return
        }

        val other = getTreeElementDepar<KtParameter>() ?: return

        val typeMatched = when (other.parent.parent) {
            is KtFunctionType, is KtCatchClause -> myMatchingVisitor.match(parameter.typeReference, other.typeReference)
            else -> matchTypeReferenceWithDeclaration(parameter.typeReference, other)
        }

        val nameIdentifierMatched = when (val handler = getHandler(parameter)) {
            is SubstitutionHandler -> handler.handle(other.nameIdentifier ?: other, myMatchingVisitor.matchContext)
            else -> matchTextOrVariable(parameter.nameIdentifier, other.nameIdentifier)
        }

        myMatchingVisitor.result = typeMatched
                && myMatchingVisitor.match(parameter.defaultValue, other.defaultValue)
                && nameIdentifierMatched
    }

    override fun visitTypeParameterList(list: KtTypeParameterList) {
        val other = getTreeElementDepar<KtTypeParameterList>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.matchSequentially(list.parameters, other.parameters)
    }

    override fun visitParameterList(list: KtParameterList) {
        val other = getTreeElementDepar<KtParameterList>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.matchSequentially(list.parameters, other.parameters)
    }

    override fun visitConstructorDelegationCall(call: KtConstructorDelegationCall) {
        val other = getTreeElementDepar<KtConstructorDelegationCall>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(call.calleeExpression, other.calleeExpression)
                && myMatchingVisitor.match(call.typeArgumentList, other.typeArgumentList)
                && matchValueArgumentList(call.valueArgumentList?.arguments, other.valueArgumentList?.arguments)
    }

    override fun visitSecondaryConstructor(constructor: KtSecondaryConstructor) {
        val other = getTreeElementDepar<KtSecondaryConstructor>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(constructor.modifierList, other.modifierList)
                && myMatchingVisitor.match(constructor.typeParameterList, other.typeParameterList)
                && myMatchingVisitor.match(constructor.valueParameterList, other.valueParameterList)
                && myMatchingVisitor.match(constructor.getDelegationCallOrNull(), other.getDelegationCallOrNull())
                && myMatchingVisitor.match(constructor.bodyExpression, other.bodyExpression)
    }

    override fun visitPrimaryConstructor(constructor: KtPrimaryConstructor) {
        val other = getTreeElementDepar<KtPrimaryConstructor>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(constructor.modifierList, other.modifierList)
                && myMatchingVisitor.match(constructor.typeParameterList, other.typeParameterList)
                && myMatchingVisitor.match(constructor.valueParameterList, other.valueParameterList)
    }

    override fun visitAnonymousInitializer(initializer: KtAnonymousInitializer) {
        val other = getTreeElementDepar<KtAnonymousInitializer>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(initializer.body, other.body)
    }

    override fun visitClassBody(classBody: KtClassBody) {
        val other = getTreeElementDepar<KtClassBody>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.matchSonsInAnyOrder(classBody, other)
    }

    override fun visitSuperTypeCallEntry(call: KtSuperTypeCallEntry) {
        val other = getTreeElementDepar<KtSuperTypeCallEntry>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(call.calleeExpression, other.calleeExpression)
                && myMatchingVisitor.match(call.typeArgumentList, other.typeArgumentList)
                && matchValueArgumentList(call.valueArgumentList?.arguments, other.valueArgumentList?.arguments)
    }

    override fun visitSuperTypeEntry(specifier: KtSuperTypeEntry) {
        val other = getTreeElementDepar<KtSuperTypeEntry>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(specifier.typeReference, other.typeReference)
    }

    override fun visitSuperTypeList(list: KtSuperTypeList) {
        val other = getTreeElementDepar<KtSuperTypeList>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.matchSonsInAnyOrder(list, other)
    }

    override fun visitClass(klass: KtClass) {
        val other = getTreeElementDepar<KtClass>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(klass.getClassOrInterfaceKeyword(), other.getClassOrInterfaceKeyword())
                && myMatchingVisitor.match(klass.modifierList, other.modifierList)
                && myMatchingVisitor.match(klass.typeParameterList, other.typeParameterList)
                && myMatchingVisitor.match(klass.primaryConstructor, other.primaryConstructor)
                && myMatchingVisitor.matchInAnyOrder(klass.secondaryConstructors, other.secondaryConstructors)
                && myMatchingVisitor.match(klass.getSuperTypeList(), other.getSuperTypeList())
                && myMatchingVisitor.match(klass.body, other.body)
                && myMatchingVisitor.match(klass.docComment, other.docComment)
                && matchTextOrVariable(klass.nameIdentifier, other.nameIdentifier) // only match identifier if others match
    }

    override fun visitObjectLiteralExpression(expression: KtObjectLiteralExpression) {
        val other = getTreeElementDepar<KtObjectLiteralExpression>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(expression.objectDeclaration, other.objectDeclaration)
    }

    override fun visitObjectDeclaration(declaration: KtObjectDeclaration) {
        val other = getTreeElementDepar<KtObjectDeclaration>() ?: return
        val otherIdentifier = if (other.isCompanion()) (other.parent.parent as KtClass).nameIdentifier else other.nameIdentifier
        myMatchingVisitor.result = myMatchingVisitor.match(declaration.modifierList, other.modifierList)
                && myMatchingVisitor.match(declaration.getSuperTypeList(), other.getSuperTypeList())
                && myMatchingVisitor.match(declaration.body, other.body)
                && matchTextOrVariable(declaration.nameIdentifier, otherIdentifier)
    }

    private fun normalizeExpressionRet(expression: KtExpression?) = if (
        expression is KtBlockExpression && expression.statements.size == 1
    ) {
        val firstExpr = expression.firstStatement
        if (firstExpr is KtReturnExpression) firstExpr.returnedExpression else firstExpr
    } else expression

    private fun normalizeExpression(expression: KtExpression?) = if (
        expression is KtBlockExpression && expression.statements.size == 1
    ) expression.firstStatement else expression

    override fun visitNamedFunction(function: KtNamedFunction) {
        val other = getTreeElementDepar<KtNamedFunction>() ?: return
        val funExpr = normalizeExpressionRet(function.bodyExpression)
        val othExpr = normalizeExpressionRet(other.bodyExpression)
        myMatchingVisitor.result = myMatchingVisitor.match(function.modifierList, other.modifierList)
                && matchTextOrVariable(function.nameIdentifier, other.nameIdentifier)
                && myMatchingVisitor.match(function.typeParameterList, other.typeParameterList)
                && matchTypeReferenceWithDeclaration(function.typeReference, other)
                && myMatchingVisitor.match(function.valueParameterList, other.valueParameterList)
                && if (funExpr == null || othExpr == null) { // both bodies are not single expression
            myMatchingVisitor.match(function.bodyExpression, other.bodyExpression)
        } else myMatchingVisitor.match(funExpr, othExpr)
    }

    override fun visitModifierList(list: KtModifierList) {
        val other = getTreeElementDepar<KtModifierList>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.matchSonsInAnyOrder(list, other)
    }

    override fun visitIfExpression(expression: KtIfExpression) {
        val other = getTreeElementDepar<KtIfExpression>() ?: return
        val elseBranch = normalizeExpression(expression.`else`)
        myMatchingVisitor.result = myMatchingVisitor.match(expression.condition, other.condition)
                && myMatchingVisitor.match(normalizeExpression(expression.then), normalizeExpression(other.then))
                && (elseBranch == null || myMatchingVisitor.match(elseBranch, normalizeExpression(other.`else`)))
    }

    override fun visitForExpression(expression: KtForExpression) {
        val other = getTreeElementDepar<KtForExpression>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(expression.loopParameter, other.loopParameter)
                && myMatchingVisitor.match(expression.loopRange, other.loopRange)
                && myMatchingVisitor.match(normalizeExpression(expression.body), normalizeExpression(other.body))
    }

    override fun visitWhileExpression(expression: KtWhileExpression) {
        val other = getTreeElementDepar<KtWhileExpression>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(expression.condition, other.condition)
                && myMatchingVisitor.match(normalizeExpression(expression.body), normalizeExpression(other.body))
    }

    override fun visitDoWhileExpression(expression: KtDoWhileExpression) {
        val other = getTreeElementDepar<KtDoWhileExpression>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(expression.condition, other.condition)
                && myMatchingVisitor.match(normalizeExpression(expression.body), normalizeExpression(other.body))
    }

    override fun visitWhenConditionInRange(condition: KtWhenConditionInRange) {
        val other = getTreeElementDepar<KtWhenConditionInRange>() ?: return
        myMatchingVisitor.result = condition.isNegated == other.isNegated
                && myMatchingVisitor.match(condition.rangeExpression, other.rangeExpression)
    }

    override fun visitWhenConditionIsPattern(condition: KtWhenConditionIsPattern) {
        val other = getTreeElementDepar<KtWhenConditionIsPattern>() ?: return
        myMatchingVisitor.result = condition.isNegated == other.isNegated
                && myMatchingVisitor.match(condition.typeReference, other.typeReference)
    }

    override fun visitWhenConditionWithExpression(condition: KtWhenConditionWithExpression) {
        val other = getTreeElementDepar<KtWhenConditionWithExpression>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(condition.expression, other.expression)
    }

    override fun visitWhenEntry(jetWhenEntry: KtWhenEntry) {
        val other = getTreeElementDepar<KtWhenEntry>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.matchInAnyOrder(jetWhenEntry.conditions, other.conditions)
                && myMatchingVisitor.match(jetWhenEntry.expression, other.expression)
    }

    override fun visitWhenExpression(expression: KtWhenExpression) {
        val other = getTreeElementDepar<KtWhenExpression>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(expression.subjectExpression, other.subjectExpression)
                && myMatchingVisitor.matchInAnyOrder(expression.entries, other.entries)
    }

    override fun visitFinallySection(finallySection: KtFinallySection) {
        val other = getTreeElementDepar<KtFinallySection>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(finallySection.finalExpression, other.finalExpression)
    }

    override fun visitCatchSection(catchClause: KtCatchClause) {
        val other = getTreeElementDepar<KtCatchClause>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(catchClause.parameterList, other.parameterList)
                && myMatchingVisitor.match(catchClause.catchBody, other.catchBody)
    }

    override fun visitTryExpression(expression: KtTryExpression) {
        val other = getTreeElementDepar<KtTryExpression>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(expression.tryBlock, other.tryBlock)
                && myMatchingVisitor.matchInAnyOrder(expression.catchClauses, other.catchClauses)
                && myMatchingVisitor.match(expression.finallyBlock, other.finallyBlock)
    }

    override fun visitTypeAlias(typeAlias: KtTypeAlias) {
        val other = getTreeElementDepar<KtTypeAlias>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(typeAlias.getTypeReference(), other.getTypeReference())
                && matchTextOrVariable(typeAlias.nameIdentifier, other.nameIdentifier)
    }

    override fun visitConstructorCalleeExpression(constructorCalleeExpression: KtConstructorCalleeExpression) {
        val other = getTreeElementDepar<KtConstructorCalleeExpression>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(constructorCalleeExpression.typeReference, other.typeReference)
                && myMatchingVisitor.match(
            constructorCalleeExpression.constructorReferenceExpression, other.constructorReferenceExpression
        )
    }

    override fun visitAnnotationEntry(annotationEntry: KtAnnotationEntry) {
        val other = getTreeElementDepar<KtAnnotationEntry>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(annotationEntry.calleeExpression, other.calleeExpression)
                && myMatchingVisitor.match(annotationEntry.typeArgumentList, other.typeArgumentList)
                && matchValueArgumentList(annotationEntry.valueArgumentList?.arguments, other.valueArgumentList?.arguments)
    }

    override fun visitProperty(property: KtProperty) {
        val other = getTreeElementDepar<KtProperty>() ?: return
        myMatchingVisitor.result = matchTypeReferenceWithDeclaration(property.typeReference, other)
                && myMatchingVisitor.match(property.modifierList, other.modifierList)
                && property.isVar == other.isVar
                && myMatchingVisitor.match(property.docComment, other.docComment)
                && (property.delegateExpressionOrInitializer == null || myMatchingVisitor.match(
            property.delegateExpressionOrInitializer, other.delegateExpressionOrInitializer
        )) && matchTextOrVariable(property.nameIdentifier, other.nameIdentifier) // match text only if others match
    }

    override fun visitStringTemplateExpression(expression: KtStringTemplateExpression) {
        val other = getTreeElementDepar<KtStringTemplateExpression>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.matchSequentially(expression.entries, other.entries)
    }

    override fun visitSimpleNameStringTemplateEntry(entry: KtSimpleNameStringTemplateEntry) {
        val other = getTreeElement<KtStringTemplateEntry>() ?: return
        val handler = getHandler(entry)
        if (handler is SubstitutionHandler) {
            myMatchingVisitor.result = handler.handle(other, myMatchingVisitor.matchContext)
            return
        }
        myMatchingVisitor.result = when (other) {
            is KtSimpleNameStringTemplateEntry, is KtBlockStringTemplateEntry ->
                myMatchingVisitor.match(entry.expression, other.expression)
            else -> false
        }
    }

    override fun visitLiteralStringTemplateEntry(entry: KtLiteralStringTemplateEntry) {
        val other = myMatchingVisitor.element
        myMatchingVisitor.result = when (val handler = entry.getUserData(CompiledPattern.HANDLER_KEY)) {
            is LiteralWithSubstitutionHandler -> handler.match(entry, other, myMatchingVisitor.matchContext)
            else -> matchTextOrVariable(entry, other)
        }
    }

    override fun visitBlockStringTemplateEntry(entry: KtBlockStringTemplateEntry) {
        val other = getTreeElementDepar<KtBlockStringTemplateEntry>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(entry.expression, other.expression)
    }

    override fun visitEscapeStringTemplateEntry(entry: KtEscapeStringTemplateEntry) {
        val other = getTreeElementDepar<KtEscapeStringTemplateEntry>() ?: return
        myMatchingVisitor.result = matchTextOrVariable(entry, other)
    }

    override fun visitBinaryWithTypeRHSExpression(expression: KtBinaryExpressionWithTypeRHS) {
        val other = getTreeElementDepar<KtBinaryExpressionWithTypeRHS>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(expression.operationReference, other.operationReference)
                && myMatchingVisitor.match(expression.left, other.left)
                && myMatchingVisitor.match(expression.right, other.right)
    }

    override fun visitIsExpression(expression: KtIsExpression) {
        val other = getTreeElementDepar<KtIsExpression>() ?: return
        myMatchingVisitor.result = expression.isNegated == other.isNegated
                && myMatchingVisitor.match(expression.leftHandSide, other.leftHandSide)
                && myMatchingVisitor.match(expression.typeReference, other.typeReference)
    }

    override fun visitDestructuringDeclaration(destructuringDeclaration: KtDestructuringDeclaration) {
        val other = getTreeElementDepar<KtDestructuringDeclaration>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.matchSequentially(destructuringDeclaration.entries, other.entries)
                && myMatchingVisitor.match(destructuringDeclaration.initializer, other.initializer)
                && myMatchingVisitor.match(destructuringDeclaration.docComment, other.docComment)
    }

    override fun visitDestructuringDeclarationEntry(multiDeclarationEntry: KtDestructuringDeclarationEntry) {
        val other = getTreeElementDepar<KtDestructuringDeclarationEntry>() ?: return
        myMatchingVisitor.result = matchTypeReferenceWithDeclaration(multiDeclarationEntry.typeReference, other)
                && myMatchingVisitor.match(multiDeclarationEntry.modifierList, other.modifierList)
                && multiDeclarationEntry.isVar == other.isVar
                && matchTextOrVariable(multiDeclarationEntry.nameIdentifier, other.nameIdentifier)
    }

    override fun visitComment(comment: PsiComment) {
        val other = getTreeElementDepar<PsiComment>() ?: return
        when (val handler = comment.getUserData(CompiledPattern.HANDLER_KEY)) {
            is LiteralWithSubstitutionHandler -> {
                myMatchingVisitor.result = handler.match(comment, other, myMatchingVisitor.matchContext)
            }
            else -> {
                myMatchingVisitor.result = myMatchingVisitor.matchText(
                    StructuralSearchUtil.normalize(getCommentText(comment)),
                    StructuralSearchUtil.normalize(getCommentText(other))
                )
            }
        }
    }

    private fun visitKDoc(kDoc: KDoc) {
        val other = getTreeElementDepar<KDoc>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.matchInAnyOrder(
            kDoc.getChildrenOfType<KDocSection>(),
            other.getChildrenOfType<KDocSection>()
        )
    }

    private fun visitKDocSection(section: KDocSection) {
        val other = getTreeElementDepar<KDocSection>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.matchSonsInAnyOrder(section, other)
    }

    private fun visitKDocTag(tag: KDocTag) {
        val other = getTreeElementDepar<KDocTag>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.matchInAnyOrder(tag.getChildrenOfType(), other.getChildrenOfType())
    }

    private fun visitKDocLink(link: KDocLink) {
        val other = getTreeElementDepar<KDocLink>() ?: return
        myMatchingVisitor.result = matchTextOrVariable(link, other)
    }
}