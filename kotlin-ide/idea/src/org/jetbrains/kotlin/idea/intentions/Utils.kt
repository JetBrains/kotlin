/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.core.getDeepestSuperDeclarations
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.core.setType
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.descriptors.JavaMethodDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.psiUtil.getCallNameExpression
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.ArrayFqNames
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.isFlexible
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.util.OperatorChecks
import org.jetbrains.kotlin.idea.references.resolveToDescriptors

fun KtContainerNode.description(): String? {
    when (node.elementType) {
        KtNodeTypes.THEN -> return "if"
        KtNodeTypes.ELSE -> return "else"
        KtNodeTypes.BODY -> {
            when (parent) {
                is KtWhileExpression -> return "while"
                is KtDoWhileExpression -> return "do...while"
                is KtForExpression -> return "for"
            }
        }
    }
    return null
}

fun KtCallExpression.isMethodCall(fqMethodName: String): Boolean {
    val resolvedCall = this.resolveToCall() ?: return false
    return resolvedCall.resultingDescriptor.fqNameUnsafe.asString() == fqMethodName
}

fun isAutoCreatedItUsage(expression: KtNameReferenceExpression) = resolveToAutoCreatedItDescriptor(expression) != null

fun resolveToAutoCreatedItDescriptor(expression: KtNameReferenceExpression): ValueParameterDescriptor? {
    if (expression.getReferencedName() != "it") return null
    val context = expression.analyze(BodyResolveMode.PARTIAL)
    val target = expression.mainReference.resolveToDescriptors(context).singleOrNull() as? ValueParameterDescriptor ?: return null
    return if (context[BindingContext.AUTO_CREATED_IT, target] == true) target else null
}

fun getLambdaByImplicitItReference(expression: KtNameReferenceExpression) =
    resolveToAutoCreatedItDescriptor(expression)?.containingDeclaration?.source?.getPsi() as? KtFunctionLiteral

// returns assignment which replaces initializer
fun splitPropertyDeclaration(property: KtProperty): KtBinaryExpression? {
    val parent = property.parent

    val initializer = property.initializer ?: return null

    val explicitTypeToSet = if (property.typeReference != null) null else initializer.analyze().getType(initializer)

    val psiFactory = KtPsiFactory(property)
    var assignment = psiFactory.createExpressionByPattern("$0 = $1", property.nameAsName!!, initializer)

    assignment = parent.addAfter(assignment, property) as KtBinaryExpression
    parent.addAfter(psiFactory.createNewLine(), property)

    property.initializer = null

    if (explicitTypeToSet != null) {
        property.setType(explicitTypeToSet)
    }

    return assignment
}

val KtQualifiedExpression.callExpression: KtCallExpression?
    get() = selectorExpression as? KtCallExpression

val KtQualifiedExpression.calleeName: String?
    get() = (callExpression?.calleeExpression as? KtNameReferenceExpression)?.text

fun KtQualifiedExpression.toResolvedCall(bodyResolveMode: BodyResolveMode): ResolvedCall<out CallableDescriptor>? {
    val callExpression = callExpression ?: return null
    return callExpression.resolveToCall(bodyResolveMode) ?: return null
}

fun KtExpression.isExitStatement(): Boolean = when (this) {
    is KtContinueExpression, is KtBreakExpression, is KtThrowExpression, is KtReturnExpression -> true
    else -> false
}

// returns false for call of super, static method or method from package
fun KtQualifiedExpression.isReceiverExpressionWithValue(): Boolean {
    val receiver = receiverExpression
    if (receiver is KtSuperExpression) return false
    return analyze().getType(receiver) != null
}

fun KtExpression.negate(reformat: Boolean = true): KtExpression {
    val specialNegation = specialNegation(reformat)
    if (specialNegation != null) return specialNegation
    return KtPsiFactory(this).createExpressionByPattern("!$0", this, reformat = reformat)
}

fun KtExpression.resultingWhens(): List<KtWhenExpression> = when (this) {
    is KtWhenExpression -> listOf(this) + entries.map { it.expression?.resultingWhens() ?: listOf() }.flatten()
    is KtIfExpression -> (then?.resultingWhens() ?: listOf()) + (`else`?.resultingWhens() ?: listOf())
    is KtBinaryExpression -> (left?.resultingWhens() ?: listOf()) + (right?.resultingWhens() ?: listOf())
    is KtUnaryExpression -> this.baseExpression?.resultingWhens() ?: listOf()
    is KtBlockExpression -> statements.lastOrNull()?.resultingWhens() ?: listOf()
    else -> listOf()
}

fun KtExpression?.hasResultingIfWithoutElse(): Boolean = when (this) {
    is KtIfExpression -> `else` == null || then.hasResultingIfWithoutElse() || `else`.hasResultingIfWithoutElse()
    is KtWhenExpression -> entries.any { it.expression.hasResultingIfWithoutElse() }
    is KtBinaryExpression -> left.hasResultingIfWithoutElse() || right.hasResultingIfWithoutElse()
    is KtUnaryExpression -> baseExpression.hasResultingIfWithoutElse()
    is KtBlockExpression -> statements.lastOrNull().hasResultingIfWithoutElse()
    else -> false
}

private fun KtExpression.specialNegation(reformat: Boolean): KtExpression? {
    val factory = KtPsiFactory(this)
    when (this) {
        is KtPrefixExpression -> {
            if (operationReference.getReferencedName() == "!") {
                val baseExpression = baseExpression
                if (baseExpression != null) {
                    val bindingContext = baseExpression.analyze(BodyResolveMode.PARTIAL)
                    val type = bindingContext.getType(baseExpression)
                    if (type != null && KotlinBuiltIns.isBoolean(type)) {
                        return KtPsiUtil.safeDeparenthesize(baseExpression)
                    }
                }
            }
        }

        is KtBinaryExpression -> {
            val operator = operationToken
            if (operator !in NEGATABLE_OPERATORS) return null
            val left = left ?: return null
            val right = right ?: return null
            return factory.createExpressionByPattern(
                "$0 $1 $2", left, getNegatedOperatorText(operator), right,
                reformat = reformat
            )
        }

        is KtIsExpression -> {
            return factory.createExpressionByPattern(
                "$0 $1 $2",
                leftHandSide,
                if (isNegated) "is" else "!is",
                typeReference ?: return null,
                reformat = reformat
            )
        }

        is KtConstantExpression -> {
            return when (text) {
                "true" -> factory.createExpression("false")
                "false" -> factory.createExpression("true")
                else -> null
            }
        }
    }
    return null
}

private val NEGATABLE_OPERATORS = setOf(
    KtTokens.EQEQ, KtTokens.EXCLEQ, KtTokens.EQEQEQ,
    KtTokens.EXCLEQEQEQ, KtTokens.IS_KEYWORD, KtTokens.NOT_IS, KtTokens.IN_KEYWORD,
    KtTokens.NOT_IN, KtTokens.LT, KtTokens.LTEQ, KtTokens.GT, KtTokens.GTEQ
)

private fun getNegatedOperatorText(token: IElementType): String {
    return when (token) {
        KtTokens.EQEQ -> KtTokens.EXCLEQ.value
        KtTokens.EXCLEQ -> KtTokens.EQEQ.value
        KtTokens.EQEQEQ -> KtTokens.EXCLEQEQEQ.value
        KtTokens.EXCLEQEQEQ -> KtTokens.EQEQEQ.value
        KtTokens.IS_KEYWORD -> KtTokens.NOT_IS.value
        KtTokens.NOT_IS -> KtTokens.IS_KEYWORD.value
        KtTokens.IN_KEYWORD -> KtTokens.NOT_IN.value
        KtTokens.NOT_IN -> KtTokens.IN_KEYWORD.value
        KtTokens.LT -> KtTokens.GTEQ.value
        KtTokens.LTEQ -> KtTokens.GT.value
        KtTokens.GT -> KtTokens.LTEQ.value
        KtTokens.GTEQ -> KtTokens.LT.value
        else -> throw IllegalArgumentException("The token $token does not have a negated equivalent.")
    }
}

internal fun KotlinType.isFlexibleRecursive(): Boolean {
    if (isFlexible()) return true
    return arguments.any { !it.isStarProjection && it.type.isFlexibleRecursive() }
}

val KtIfExpression.branches: List<KtExpression?> get() = ifBranchesOrThis()

private fun KtExpression.ifBranchesOrThis(): List<KtExpression?> {
    if (this !is KtIfExpression) return listOf(this)
    return listOf(then) + `else`?.ifBranchesOrThis().orEmpty()
}

fun ResolvedCall<out CallableDescriptor>.resolvedToArrayType(): Boolean =
    resultingDescriptor.returnType.let { type ->
        type != null && (KotlinBuiltIns.isArray(type) || KotlinBuiltIns.isPrimitiveArray(type))
    }

fun KtElement?.isZero() = this?.text == "0"

fun KtElement?.isOne() = this?.text == "1"

private fun KtExpression.isExpressionOfTypeOrSubtype(predicate: (KotlinType) -> Boolean): Boolean {
    val returnType = resolveToCall()?.resultingDescriptor?.returnType
    return returnType != null && (returnType.constructor.supertypes + returnType).any(predicate)
}

fun KtElement?.isSizeOrLength(): Boolean {
    if (this !is KtDotQualifiedExpression) return false

    return when (selectorExpression?.text) {
        "size" -> receiverExpression.isExpressionOfTypeOrSubtype { type ->
            KotlinBuiltIns.isArray(type) ||
                    KotlinBuiltIns.isPrimitiveArray(type) ||
                    KotlinBuiltIns.isCollectionOrNullableCollection(type) ||
                    KotlinBuiltIns.isMapOrNullableMap(type)
        }
        "length" -> receiverExpression.isExpressionOfTypeOrSubtype(KotlinBuiltIns::isCharSequenceOrNullableCharSequence)
        else -> false
    }
}


fun KtDotQualifiedExpression.getLeftMostReceiverExpression(): KtExpression =
    (receiverExpression as? KtDotQualifiedExpression)?.getLeftMostReceiverExpression() ?: receiverExpression

fun KtDotQualifiedExpression.replaceFirstReceiver(
    factory: KtPsiFactory,
    newReceiver: KtExpression,
    safeAccess: Boolean = false
): KtExpression {
    val replaced = (if (safeAccess) {
        this.replaced(factory.createExpressionByPattern("$0?.$1", receiverExpression, selectorExpression!!))
    } else this) as KtQualifiedExpression
    val receiver = replaced.receiverExpression
    when (receiver) {
        is KtDotQualifiedExpression -> {
            receiver.replace(receiver.replaceFirstReceiver(factory, newReceiver, safeAccess))
        }
        else -> {
            receiver.replace(newReceiver)
        }
    }
    return replaced
}

fun KtDotQualifiedExpression.deleteFirstReceiver(): KtExpression {
    val receiver = receiverExpression
    when (receiver) {
        is KtDotQualifiedExpression -> receiver.deleteFirstReceiver()
        else -> selectorExpression?.let { return this.replace(it) as KtExpression }
    }
    return this
}

private val ARRAY_OF_METHODS = setOf(ArrayFqNames.ARRAY_OF_FUNCTION) +
        ArrayFqNames.PRIMITIVE_TYPE_TO_ARRAY.values.toSet() +
        Name.identifier("emptyArray")

fun KtCallExpression.isArrayOfMethod(): Boolean {
    val resolvedCall = resolveToCall() ?: return false
    val descriptor = resolvedCall.candidateDescriptor
    return (descriptor.containingDeclaration as? PackageFragmentDescriptor)?.fqName == KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME &&
            ARRAY_OF_METHODS.contains(descriptor.name)
}

fun KtBlockExpression.getParentLambdaLabelName(): String? {
    val lambdaExpression = getStrictParentOfType<KtLambdaExpression>() ?: return null
    val callExpression = lambdaExpression.getStrictParentOfType<KtCallExpression>() ?: return null
    val valueArgument = callExpression.valueArguments.find {
        it.getArgumentExpression()?.unpackFunctionLiteral(allowParentheses = false) === lambdaExpression
    } ?: return null
    val lambdaLabelName = (valueArgument.getArgumentExpression() as? KtLabeledExpression)?.getLabelName()
    return lambdaLabelName ?: callExpression.getCallNameExpression()?.text
}

internal fun KtExpression.getCallableDescriptor() = resolveToCall()?.resultingDescriptor

fun KtDeclaration.isFinalizeMethod(descriptor: DeclarationDescriptor? = null): Boolean {
    if (containingClass() == null) return false
    val function = this as? KtNamedFunction ?: return false
    return function.name == "finalize"
            && function.valueParameters.isEmpty()
            && ((descriptor ?: function.descriptor) as? FunctionDescriptor)?.returnType?.isUnit() == true
}

fun KtDotQualifiedExpression.isToString(): Boolean {
    val callExpression = selectorExpression as? KtCallExpression ?: return false
    val referenceExpression = callExpression.calleeExpression as? KtNameReferenceExpression ?: return false
    if (referenceExpression.getReferencedName() != "toString") return false
    val resolvedCall = toResolvedCall(BodyResolveMode.PARTIAL) ?: return false
    val callableDescriptor = resolvedCall.resultingDescriptor as? CallableMemberDescriptor ?: return false
    return callableDescriptor.getDeepestSuperDeclarations().any { it.fqNameUnsafe.asString() == "kotlin.Any.toString" }
}

val FunctionDescriptor.isOperatorOrCompatible: Boolean
    get() {
        if (this is JavaMethodDescriptor) {
            return OperatorChecks.check(this).isSuccess
        }
        return isOperator
    }

fun KtPsiFactory.appendSemicolonBeforeLambdaContainingElement(element: PsiElement) {
    val previousElement = KtPsiUtil.skipSiblingsBackwardByPredicate(element) {
        it!!.node.elementType in KtTokens.WHITE_SPACE_OR_COMMENT_BIT_SET
    }
    if (previousElement != null && previousElement is KtExpression) {
        previousElement.parent.addAfter(createSemicolon(), previousElement)
    }
}

internal fun Sequence<PsiElement>.lastWithPersistedElementOrNull(elementShouldPersist: KtExpression): PsiElement? {
    var lastElement: PsiElement? = null
    var checked = false

    for (element in this) {
        checked = checked || (element === elementShouldPersist)
        lastElement = element
    }

    return if (checked) lastElement else null
}
