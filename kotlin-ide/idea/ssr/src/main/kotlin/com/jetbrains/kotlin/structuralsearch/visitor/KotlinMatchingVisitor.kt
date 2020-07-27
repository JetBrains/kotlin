package com.jetbrains.kotlin.structuralsearch.visitor

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.structuralsearch.StructuralSearchUtil
import com.intellij.structuralsearch.impl.matcher.CompiledPattern
import com.intellij.structuralsearch.impl.matcher.GlobalMatchingVisitor
import com.intellij.structuralsearch.impl.matcher.handlers.LiteralWithSubstitutionHandler
import com.intellij.structuralsearch.impl.matcher.handlers.SubstitutionHandler
import com.intellij.util.containers.reverse
import com.jetbrains.kotlin.structuralsearch.binaryExprOpName
import com.jetbrains.kotlin.structuralsearch.getCommentText
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.fir.builder.toUnaryName
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.debugger.sequence.psi.resolveType
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.idea.intentions.calleeName
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.kdoc.psi.impl.KDocImpl
import org.jetbrains.kotlin.kdoc.psi.impl.KDocLink
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.nj2k.postProcessing.type
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.jetbrains.kotlin.psi.psiUtil.referenceExpression
import org.jetbrains.kotlin.psi2ir.deparenthesize
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.source.getPsi
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

    private fun GlobalMatchingVisitor.matchNormalized(
        element: KtExpression?,
        element2: KtExpression?,
        returnExpr: Boolean = false
    ): Boolean {
        val (e1, e2) =
            if (element is KtBlockExpression && element2 is KtBlockExpression) element to element2
            else normalizeExpressions(element, element2, returnExpr)

        val impossible = e1?.let {
            val handler = getHandler(it)
            e2 !is KtBlockExpression && handler is SubstitutionHandler && handler.minOccurs > 1
        } ?: false

        return !impossible && match(e1, e2)
    }

    private fun getHandler(element: PsiElement) = myMatchingVisitor.matchContext.pattern.getHandler(element)

    private fun matchTextOrVariable(el1: PsiElement?, el2: PsiElement?): Boolean {
        if (el1 == null || el2 == null) return el1 == el2
        return when (val handler = getHandler(el1)) {
            is SubstitutionHandler -> handler.validate(el2, myMatchingVisitor.matchContext)
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
            KtTokens.IDENTIFIER -> myMatchingVisitor.result = matchTextOrVariable(element, other)
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
                val token = expression.operationToken
                if (token in augmentedAssignmentsMap.keys && other.operationToken == KtTokens.EQ) {
                    // Matching x ?= y with x = x ? y
                    val right = other.right?.deparenthesize()
                    val left = other.left?.deparenthesize()
                    myMatchingVisitor.result = right is KtBinaryExpression
                            && augmentedAssignmentsMap[token] == right.operationToken
                            && myMatchingVisitor.match(expression.left, left)
                            && myMatchingVisitor.match(expression.left, right.left)
                            && myMatchingVisitor.match(expression.right, right.right)
                    return
                }
                if (token == KtTokens.EQ) {
                    val right = expression.right?.deparenthesize()
                    if (right is KtBinaryExpression && right.operationToken == augmentedAssignmentsMap[other.operationToken]) {
                        // Matching x = x ? y with x ?= y
                        myMatchingVisitor.result = myMatchingVisitor.match(expression.left, other.left)
                                && myMatchingVisitor.match(right.left, other.left)
                                && myMatchingVisitor.match(right.right, other.right)
                        return
                    }
                }
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
                        if (expression.operationToken == KtTokens.EQ && right is KtBinaryExpression) {
                            // Matching x = x + y with x.plusAssign(y)
                            val opName = augmentedAssignmentsMap.reverse()[right.operationToken]?.binaryExprOpName()
                            myMatchingVisitor.result = selector is KtCallExpression
                                    && myMatchingVisitor.match(left, other.receiverExpression)
                                    && other.match(opName, right.left, right.right)
                        } else {
                            myMatchingVisitor.result = selector is KtCallExpression && other.match(
                                expression.operationToken.binaryExprOpName(), left, right
                            )
                        }
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
        myMatchingVisitor.result = myMatchingVisitor.matchSequentially(expression.statements, other.statements)
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
        val other = getTreeElementDepar<KtExpression>() ?: return
        myMatchingVisitor.result = matchTextOrVariable(expression, other)
        val handler = getHandler(expression)
        if (myMatchingVisitor.result && handler is SubstitutionHandler) {
            handler.handle(other, myMatchingVisitor.matchContext)
        }
    }

    override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
        val other = getTreeElementDepar<PsiElement>() ?: return
        myMatchingVisitor.result = matchTextOrVariable(
            expression.getReferencedNameElement(),
            if (other is KtSimpleNameExpression) other.getReferencedNameElement() else other
        )
        val handler = getHandler(expression.getReferencedNameElement())
        if (myMatchingVisitor.result && handler is SubstitutionHandler) {
            if (handler.maxOccurs == 0) {
                myMatchingVisitor.result = false
                return
            }
            handler.handle(
                if (other is KtSimpleNameExpression) other.getReferencedNameElement() else other,
                myMatchingVisitor.matchContext
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
                && myMatchingVisitor.match(type.returnTypeReference, other.returnTypeReference)
    }

    override fun visitUserType(type: KtUserType) {
        val other = myMatchingVisitor.element

        myMatchingVisitor.result = when (other) {
            is KtUserType -> {
                type.qualifier?.let { typeQualifier -> // if query has fq type
                    myMatchingVisitor.match(typeQualifier, other.qualifier) // recursively match qualifiers
                            && myMatchingVisitor.match(type.referenceExpression, other.referenceExpression)
                            && myMatchingVisitor.match(type.typeArgumentList, other.typeArgumentList)
                } ?: let { // no fq type
                    myMatchingVisitor.match(type.referenceExpression, other.referenceExpression)
                            && myMatchingVisitor.match(type.typeArgumentList, other.typeArgumentList)
                }
            }
            is KtTypeElement -> matchTextOrVariable(type.referenceExpression, other)
            else -> false
        }
    }

    override fun visitNullableType(nullableType: KtNullableType) {
        val other = getTreeElementDepar<KtNullableType>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(nullableType.innerType, other.innerType)
    }

    override fun visitDynamicType(type: KtDynamicType) {
        myMatchingVisitor.result = myMatchingVisitor.element is KtDynamicType
    }

    private fun matchTypeReferenceWithDeclaration(typeReference: KtTypeReference?, other: KtDeclaration): Boolean {
        val type = other.type()
        if (type != null) {
            val fqType = DescriptorRenderer.DEBUG_TEXT.renderType(type)
            val factory = KtPsiFactory(other, true)
            return myMatchingVisitor.match(typeReference, factory.createTypeIfPossible(fqType))
        }
        return false
    }

    override fun visitTypeReference(typeReference: KtTypeReference) {
        val other = getTreeElementDepar<KtTypeReference>() ?: return

        var fqMatch = false

        val parent = other.parent
        val type = try {
            when {
                parent is KtDeclaration && parent.descriptor is FunctionDescriptor ->
                    (parent.descriptor as FunctionDescriptor).extensionReceiverParameter?.value?.type
                parent is KtDeclaration && parent.descriptor is PropertyDescriptorImpl ->
                    (parent.descriptor as PropertyDescriptorImpl).extensionReceiverParameter?.value?.type
                else -> null
            }
        } catch (t: Throwable) {
            null
        }

        if (type != null) {
            val handler = getHandler(typeReference)
            fqMatch = if (handler is SubstitutionHandler) {
                if (handler.findRegExpPredicate()?.doMatch(
                        DescriptorRenderer.DEBUG_TEXT.renderType(type), myMatchingVisitor.matchContext, other
                    ) == true
                ) {
                    handler.addResult(other, myMatchingVisitor.matchContext)
                    true
                } else false
            } else {
                myMatchingVisitor.matchText(typeReference.text, DescriptorRenderer.DEBUG_TEXT.renderType(type))
            }
        }

        myMatchingVisitor.result = fqMatch || myMatchingVisitor.matchSons(typeReference, other)
    }

    override fun visitQualifiedExpression(expression: KtQualifiedExpression) {
        val other = getTreeElementDepar<KtQualifiedExpression>() ?: return
        myMatchingVisitor.result = expression.operationSign == other.operationSign
                && myMatchingVisitor.match(expression.receiverExpression, other.receiverExpression)
                && myMatchingVisitor.match(expression.selectorExpression, other.selectorExpression)
    }

    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
        val other = getTreeElementDepar<KtExpression>() ?: return
        val handler = getHandler(expression.receiverExpression)
        if (other is KtDotQualifiedExpression && handler is SubstitutionHandler && handler.maxOccurs == 0) {
            // Don't match '_{0,0}.'_
            myMatchingVisitor.result = false
        } else if (other is KtDotQualifiedExpression) {
            // Regular matching
            myMatchingVisitor.result = myMatchingVisitor.match(expression.receiverExpression, other.receiverExpression)
                    && myMatchingVisitor.match(expression.selectorExpression, other.selectorExpression)
        } else {
            // Match '_?.'_
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
                && myMatchingVisitor.match(typeProjection.modifierList, other.modifierList)
                && typeProjection.projectionKind == other.projectionKind
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

    private fun MutableList<KtValueArgument>.addDefaultArguments(parameters: List<KtParameter?>) {
        if (parameters.isEmpty()) return
        val params = parameters.toTypedArray()
        var i = 0
        while (i < size) {
            val arg = get(i)
            if (arg.isNamed()) {
                params[parameters.indexOfFirst { it?.nameAsName == arg.getArgumentName()?.asName }] = null
                i++
            } else {
                val curParam = params[i] ?: throw IllegalStateException(
                    "Param can't be null at index $i in ${params.map { it?.text }}."
                )
                params[i] = null
                if (curParam.isVarArg) {
                    val varArgType = arg.getArgumentExpression()?.resolveType()
                    var curArg: KtValueArgument? = arg
                    while (varArgType == curArg?.getArgumentExpression()?.resolveType() || curArg?.isSpread == true) {
                        i++
                        curArg = getOrNull(i)

                    }
                } else i++
            }
        }
        val psiFactory = KtPsiFactory(myMatchingVisitor.element)
        params.filterNotNull().forEach { add(psiFactory.createArgument(it.defaultValue, it.nameAsName, reformat = false)) }
    }

    private fun matchValueArguments(
        parameters: List<KtParameter>,
        valueArgList: KtValueArgumentList?,
        otherValueArgList: KtValueArgumentList?,
        lambdaArgList: List<KtLambdaArgument>,
        otherLambdaArgList: List<KtLambdaArgument>
    ): Boolean {
        if (valueArgList != null) {
            val handler = getHandler(valueArgList)
            val normalizedOtherArgs = otherValueArgList?.arguments?.toMutableList() ?: mutableListOf()
            normalizedOtherArgs.addAll(otherLambdaArgList)
            normalizedOtherArgs.addDefaultArguments(parameters)
            if (normalizedOtherArgs.isEmpty() && handler is SubstitutionHandler && handler.minOccurs == 0) {
                return myMatchingVisitor.matchSequentially(lambdaArgList, otherLambdaArgList)
            }
            val normalizedArgs = valueArgList.arguments.toMutableList()
            normalizedArgs.addAll(lambdaArgList)
            return matchValueArguments(normalizedArgs, normalizedOtherArgs)
        }
        return matchValueArguments(lambdaArgList, otherLambdaArgList)
    }

    private fun matchValueArguments(queryArgs: List<KtValueArgument>, codeArgs: List<KtValueArgument>): Boolean {
        var queryIndex = 0
        var codeIndex = 0
        while (queryIndex < queryArgs.size) {
            val queryArg = queryArgs[queryIndex]
            val codeArg = codeArgs.getOrElse(codeIndex) { return@matchValueArguments false }
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
                        if (!myMatchingVisitor.match(spreadedArg, codeArgs[codeIndex++])) return@matchValueArguments false
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
                        if (!myMatchingVisitor.match(queryArgs[queryIndex++], spreadedArg)) return@matchValueArguments false
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

    private fun resolveParameters(other: KtElement): List<KtParameter> = try {
        other.resolveToCall()?.candidateDescriptor?.original?.valueParameters?.map {
            it.source.getPsi() as KtParameter
        } ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }

    override fun visitCallExpression(expression: KtCallExpression) {
        val other = getTreeElementDepar<KtExpression>() ?: return
        val parameters = resolveParameters(other)
        myMatchingVisitor.result = when (other) {
            is KtCallExpression -> {
                myMatchingVisitor.match(expression.calleeExpression, other.calleeExpression)
                        && myMatchingVisitor.match(expression.typeArgumentList, other.typeArgumentList)
                        && matchValueArguments(
                    parameters, expression.valueArgumentList, other.valueArgumentList,
                    expression.lambdaArguments, other.lambdaArguments
                )
            }
            is KtDotQualifiedExpression -> myMatchingVisitor.match(expression.calleeExpression, other.receiverExpression)
                    && other.calleeName == "${OperatorNameConventions.INVOKE}"
                    && matchValueArguments(
                parameters,
                expression.valueArgumentList, other.callExpression?.valueArgumentList,
                expression.lambdaArguments, other.callExpression?.lambdaArguments ?: emptyList()
            )
            else -> false
        }
    }

    override fun visitCallableReferenceExpression(expression: KtCallableReferenceExpression) {
        val other = getTreeElementDepar<KtCallableReferenceExpression>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(expression.callableReference, other.callableReference)
                && myMatchingVisitor.match(expression.receiverExpression, other.receiverExpression)
    }

    override fun visitTypeParameter(parameter: KtTypeParameter) {
        val other = getTreeElementDepar<KtTypeParameter>() ?: return
        myMatchingVisitor.result = matchTextOrVariable(parameter.firstChild, other.firstChild) // match generic identifier
                && myMatchingVisitor.match(parameter.extendsBound, other.extendsBound)
                && parameter.variance == other.variance
        parameter.nameIdentifier?.let { nameIdentifier ->
            val handler = getHandler(nameIdentifier)
            if (myMatchingVisitor.result && handler is SubstitutionHandler) {
                handler.handle(other.nameIdentifier, myMatchingVisitor.matchContext)
            }
        }
    }

    override fun visitParameter(parameter: KtParameter) {
        val other = getTreeElementDepar<KtParameter>() ?: return
        val typeMatched = when (other.parent.parent) {
            is KtFunctionType, is KtCatchClause -> myMatchingVisitor.match(parameter.typeReference, other.typeReference)
            else -> matchTypeReferenceWithDeclaration(parameter.typeReference, other)
        }
        val otherNameIdentifier = if (getHandler(parameter) is SubstitutionHandler
            && parameter.nameIdentifier != null
            && other.nameIdentifier == null
        ) other else other.nameIdentifier
        myMatchingVisitor.result = typeMatched
                && myMatchingVisitor.match(parameter.defaultValue, other.defaultValue)
                && myMatchingVisitor.match(parameter.valOrVarKeyword, other.valOrVarKeyword)
                && (parameter.nameIdentifier == null || matchTextOrVariable(parameter.nameIdentifier, otherNameIdentifier))
                && myMatchingVisitor.match(parameter.modifierList, other.modifierList)
                && myMatchingVisitor.match(parameter.destructuringDeclaration, other.destructuringDeclaration)

        parameter.nameIdentifier?.let { nameIdentifier ->
            val handler = getHandler(nameIdentifier)
            if (myMatchingVisitor.result && handler is SubstitutionHandler) {
                handler.handle(other.nameIdentifier, myMatchingVisitor.matchContext)
            }
        }
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
        val parameters = resolveParameters(other)
        myMatchingVisitor.result = myMatchingVisitor.match(call.calleeExpression, other.calleeExpression)
                && myMatchingVisitor.match(call.typeArgumentList, other.typeArgumentList)
                && matchValueArguments(
            parameters,
            call.valueArgumentList, other.valueArgumentList, call.lambdaArguments, other.lambdaArguments
        )
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
        val parameters = resolveParameters(other)
        myMatchingVisitor.result = myMatchingVisitor.match(call.calleeExpression, other.calleeExpression)
                && myMatchingVisitor.match(call.typeArgumentList, other.typeArgumentList)
                && matchValueArguments(
            parameters,
            call.valueArgumentList, other.valueArgumentList, call.lambdaArguments, other.lambdaArguments
        )
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
                && matchTextOrVariable(klass.nameIdentifier, other.nameIdentifier)
                && myMatchingVisitor.match(klass.typeParameterList, other.typeParameterList)
                && myMatchingVisitor.match(klass.primaryConstructor, other.primaryConstructor)
                && myMatchingVisitor.matchInAnyOrder(klass.secondaryConstructors, other.secondaryConstructors)
                && myMatchingVisitor.match(klass.getSuperTypeList(), other.getSuperTypeList())
                && myMatchingVisitor.match(klass.body, other.body)
                && myMatchingVisitor.match(klass.docComment, other.docComment)
        val handler = getHandler(klass.nameIdentifier!!)
        if (myMatchingVisitor.result && handler is SubstitutionHandler) {
            handler.handle(other.nameIdentifier, myMatchingVisitor.matchContext)
        }
    }

    override fun visitObjectLiteralExpression(expression: KtObjectLiteralExpression) {
        val other = getTreeElementDepar<KtObjectLiteralExpression>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(expression.objectDeclaration, other.objectDeclaration)
    }

    override fun visitObjectDeclaration(declaration: KtObjectDeclaration) {
        val other = getTreeElementDepar<KtObjectDeclaration>() ?: return
        val otherIdentifier = if (other.isCompanion()) (other.parent.parent as KtClass).nameIdentifier else other.nameIdentifier
        myMatchingVisitor.result = myMatchingVisitor.match(declaration.modifierList, other.modifierList)
                && matchTextOrVariable(declaration.nameIdentifier, otherIdentifier)
                && myMatchingVisitor.match(declaration.getSuperTypeList(), other.getSuperTypeList())
                && myMatchingVisitor.match(declaration.body, other.body)
        declaration.nameIdentifier?.let { declNameIdentifier ->
            val handler = getHandler(declNameIdentifier)
            if (myMatchingVisitor.result && handler is SubstitutionHandler) {
                handler.handle(otherIdentifier, myMatchingVisitor.matchContext)
            }
        }
    }

    private fun normalizeExpressionRet(expression: KtExpression?): KtExpression? = when {
        expression is KtBlockExpression && expression.statements.size == 1 -> expression.firstStatement?.let {
            if (it is KtReturnExpression) it.returnedExpression else it
        }
        else -> expression
    }

    private fun normalizeExpression(expression: KtExpression?): KtExpression? = when {
        expression is KtBlockExpression && expression.statements.size == 1 -> expression.firstStatement
        else -> expression
    }

    private fun normalizeExpressions(
        patternExpr: KtExpression?,
        codeExpr: KtExpression?,
        returnExpr: Boolean
    ): Pair<KtExpression?, KtExpression?> {
        val normalizedExpr = if (returnExpr) normalizeExpressionRet(patternExpr) else normalizeExpression(patternExpr)
        val normalizedCodeExpr = if (returnExpr) normalizeExpressionRet(codeExpr) else normalizeExpression(codeExpr)

        return when {
            normalizedExpr is KtBlockExpression || normalizedCodeExpr is KtBlockExpression -> patternExpr to codeExpr
            else -> normalizedExpr to normalizedCodeExpr
        }
    }

    override fun visitNamedFunction(function: KtNamedFunction) {
        val other = getTreeElementDepar<KtNamedFunction>() ?: return
        val (patternBody, codeBody) = normalizeExpressions(function.bodyBlockExpression, other.bodyBlockExpression, true)

        val bodyHandler = patternBody?.let(::getHandler)
        val bodyMatch = when {
            patternBody is KtNameReferenceExpression && codeBody == null -> bodyHandler is SubstitutionHandler
                    && bodyHandler.minOccurs <= 1 && bodyHandler.maxOccurs >= 1
                    && myMatchingVisitor.match(patternBody, other.bodyExpression)
            patternBody is KtNameReferenceExpression -> myMatchingVisitor.match(
                function.bodyBlockExpression,
                other.bodyBlockExpression
            )
            patternBody == null && codeBody == null -> myMatchingVisitor.match(function.bodyExpression, other.bodyExpression)
            patternBody == null -> myMatchingVisitor.match(function.bodyExpression, codeBody)
            codeBody == null -> myMatchingVisitor.match(patternBody, other.bodyExpression)
            else -> myMatchingVisitor.match(function.bodyBlockExpression, other.bodyBlockExpression)
        }

        myMatchingVisitor.result = myMatchingVisitor.match(function.modifierList, other.modifierList)
                && matchTextOrVariable(function.nameIdentifier, other.nameIdentifier)
                && myMatchingVisitor.match(function.typeParameterList, other.typeParameterList)
                && matchTypeReferenceWithDeclaration(function.typeReference, other)
                && myMatchingVisitor.match(function.valueParameterList, other.valueParameterList)
                && myMatchingVisitor.match(function.receiverTypeReference, other.receiverTypeReference)
                && bodyMatch

        function.nameIdentifier?.let { nameIdentifier ->
            val handler = getHandler(nameIdentifier)
            if (myMatchingVisitor.result && handler is SubstitutionHandler) {
                handler.handle(other.nameIdentifier, myMatchingVisitor.matchContext)
            }
        }
    }

    override fun visitModifierList(list: KtModifierList) {
        val other = getTreeElementDepar<KtModifierList>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.matchSonsInAnyOrder(list, other)
    }

    override fun visitIfExpression(expression: KtIfExpression) {
        val other = getTreeElementDepar<KtIfExpression>() ?: return
        val elseBranch = normalizeExpression(expression.`else`)
        myMatchingVisitor.result = myMatchingVisitor.match(expression.condition, other.condition)
                && myMatchingVisitor.matchNormalized(expression.then, other.then)
                && (elseBranch == null || myMatchingVisitor.matchNormalized(expression.`else`, other.`else`))
    }

    override fun visitForExpression(expression: KtForExpression) {
        val other = getTreeElementDepar<KtForExpression>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(expression.loopParameter, other.loopParameter)
                && myMatchingVisitor.match(expression.loopRange, other.loopRange)
                && myMatchingVisitor.matchNormalized(expression.body, other.body)
    }

    override fun visitWhileExpression(expression: KtWhileExpression) {
        val other = getTreeElementDepar<KtWhileExpression>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(expression.condition, other.condition)
                && myMatchingVisitor.matchNormalized(expression.body, other.body)
    }

    override fun visitDoWhileExpression(expression: KtDoWhileExpression) {
        val other = getTreeElementDepar<KtDoWhileExpression>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(expression.condition, other.condition)
                && myMatchingVisitor.matchNormalized(expression.body, other.body)
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
        val handler = getHandler(condition)
        if (handler is SubstitutionHandler) {
            myMatchingVisitor.result = handler.handle(myMatchingVisitor.element, myMatchingVisitor.matchContext)
        } else {
            val other = getTreeElementDepar<KtWhenConditionWithExpression>() ?: return
            myMatchingVisitor.result = myMatchingVisitor.match(condition.expression, other.expression)
        }
    }

    override fun visitWhenEntry(jetWhenEntry: KtWhenEntry) {
        val other = getTreeElementDepar<KtWhenEntry>() ?: return

        // $x$ -> $y$ should match else branches
        val bypassElseTest = jetWhenEntry.firstChild is KtWhenConditionWithExpression
                && jetWhenEntry.firstChild.children.size == 1
                && jetWhenEntry.firstChild.firstChild is KtNameReferenceExpression

        myMatchingVisitor.result =
            (bypassElseTest && other.isElse || myMatchingVisitor.matchInAnyOrder(jetWhenEntry.conditions, other.conditions))
                    && myMatchingVisitor.match(jetWhenEntry.expression, other.expression)
                    && (bypassElseTest || jetWhenEntry.isElse == other.isElse)
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
        myMatchingVisitor.result = matchTextOrVariable(typeAlias.nameIdentifier, other.nameIdentifier)
                && myMatchingVisitor.match(typeAlias.getTypeReference(), other.getTypeReference())
                && myMatchingVisitor.matchInAnyOrder(typeAlias.annotationEntries, other.annotationEntries)
        val handler = getHandler(typeAlias.nameIdentifier!!)
        if (myMatchingVisitor.result && handler is SubstitutionHandler) {
            handler.handle(other.nameIdentifier, myMatchingVisitor.matchContext)
        }
    }

    override fun visitConstructorCalleeExpression(constructorCalleeExpression: KtConstructorCalleeExpression) {
        val other = getTreeElementDepar<KtConstructorCalleeExpression>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(
            constructorCalleeExpression.constructorReferenceExpression, other.constructorReferenceExpression
        )
    }

    override fun visitAnnotationEntry(annotationEntry: KtAnnotationEntry) {
        val other = getTreeElementDepar<KtAnnotationEntry>() ?: return
        val parameters = resolveParameters(other)
        myMatchingVisitor.result = myMatchingVisitor.match(annotationEntry.calleeExpression, other.calleeExpression)
                && myMatchingVisitor.match(annotationEntry.typeArgumentList, other.typeArgumentList)
                && matchValueArguments(
            parameters,
            annotationEntry.valueArgumentList, other.valueArgumentList, annotationEntry.lambdaArguments, other.lambdaArguments
        )
                && matchTextOrVariable(annotationEntry.useSiteTarget, other.useSiteTarget)
    }

    override fun visitAnnotatedExpression(expression: KtAnnotatedExpression) {
        when (val other = myMatchingVisitor.element) {
            is KtAnnotatedExpression -> myMatchingVisitor.result =
                if (expression.annotationEntries.all
                    { val handler = getHandler(it); handler is SubstitutionHandler && handler.maxOccurs == 0 }
                    && other.annotationEntries.any()
                ) false
                else myMatchingVisitor.match(expression.baseExpression, other.baseExpression)
                        && myMatchingVisitor.matchInAnyOrder(expression.annotationEntries, other.annotationEntries)
            else -> {
                myMatchingVisitor.result = if (expression.annotationEntries.all
                    { val handler = getHandler(it); handler is SubstitutionHandler && handler.minOccurs == 0 }
                ) myMatchingVisitor.match(expression.baseExpression, other) else false
            }
        }
    }

    override fun visitProperty(property: KtProperty) {
        val other = getTreeElementDepar<KtProperty>() ?: return
        myMatchingVisitor.result = matchTypeReferenceWithDeclaration(property.typeReference, other)
                && myMatchingVisitor.match(property.modifierList, other.modifierList)
                && matchTextOrVariable(property.nameIdentifier, other.nameIdentifier)
                && property.isVar == other.isVar
                && myMatchingVisitor.match(property.docComment, other.docComment)
                && (property.delegateExpressionOrInitializer == null || myMatchingVisitor.matchOptionally(
            property.delegateExpressionOrInitializer, other.delegateExpressionOrInitializer
        ))
                && myMatchingVisitor.match(property.getter, other.getter)
                && myMatchingVisitor.match(property.setter, other.setter)
                && myMatchingVisitor.match(property.receiverTypeReference, other.receiverTypeReference)
        val handler = getHandler(property.nameIdentifier!!)
        if (myMatchingVisitor.result && handler is SubstitutionHandler) {
            handler.handle(other.nameIdentifier, myMatchingVisitor.matchContext)
        }
    }

    override fun visitPropertyAccessor(accessor: KtPropertyAccessor) {
        val other = getTreeElementDepar<KtPropertyAccessor>() ?: return
        val accessorBody = if (accessor.hasBlockBody()) accessor.bodyBlockExpression else accessor.bodyExpression
        val otherBody = if (other.hasBlockBody()) other.bodyBlockExpression else other.bodyExpression
        myMatchingVisitor.result = myMatchingVisitor.match(accessor.modifierList, other.modifierList)
                && myMatchingVisitor.matchNormalized(accessorBody, otherBody, true)
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

    override fun visitThrowExpression(expression: KtThrowExpression) {
        val other = getTreeElementDepar<KtThrowExpression>() ?: return
        myMatchingVisitor.result = myMatchingVisitor.match(expression.referenceExpression(), other.referenceExpression())
    }

    override fun visitComment(comment: PsiComment) {
        val other = getTreeElementDepar<PsiComment>() ?: return
        when (val handler = comment.getUserData(CompiledPattern.HANDLER_KEY)) {
            is LiteralWithSubstitutionHandler -> {
                if (other is KDocImpl) {
                    myMatchingVisitor.result = handler.match(comment, other, myMatchingVisitor.matchContext)
                } else {
                    val offset = 2 + other.text.substring(2).indexOfFirst { it > ' ' }
                    myMatchingVisitor.result = handler.match(other, getCommentText(other), offset, myMatchingVisitor.matchContext)
                }
            }
            is SubstitutionHandler -> {
                handler.findRegExpPredicate()?.let {
                    it.setNodeTextGenerator { comment -> getCommentText(comment as PsiComment) }
                }
                myMatchingVisitor.result = handler.handle(
                    other,
                    2,
                    other.textLength - if (other.tokenType == KtTokens.EOL_COMMENT) 0 else 2,
                    myMatchingVisitor.matchContext
                )
            }
            else -> myMatchingVisitor.result = myMatchingVisitor.matchText(
                StructuralSearchUtil.normalize(getCommentText(comment)),
                StructuralSearchUtil.normalize(getCommentText(other))
            )
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

    companion object {
        private val augmentedAssignmentsMap = mapOf(
            KtTokens.PLUSEQ to KtTokens.PLUS,
            KtTokens.MINUSEQ to KtTokens.MINUS,
            KtTokens.MULTEQ to KtTokens.MUL,
            KtTokens.DIVEQ to KtTokens.DIV,
            KtTokens.PERCEQ to KtTokens.PERC
        )
    }
}