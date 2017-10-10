package org.jetbrains.uast.kotlin.expressions

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.CommonSupertypes
import org.jetbrains.uast.*
import org.jetbrains.uast.kotlin.*
import org.jetbrains.uast.kotlin.kinds.KotlinSpecialExpressionKinds
import org.jetbrains.uast.kotlin.psi.UastKotlinPsiVariable


private fun createVariableReferenceExpression(variable: UVariable, containingElement: UElement?) =
        object : USimpleNameReferenceExpression, JvmDeclarationUElement {
            override val psi: PsiElement? = null
            override fun resolve(): PsiElement? = variable
            override val uastParent: UElement? = containingElement
            override val resolvedName: String? = variable.name
            override val annotations: List<UAnnotation> = emptyList()
            override val identifier: String = variable.name.orAnonymous()
            override val javaPsi: PsiElement? = null
            override val sourcePsi: PsiElement? = null
        }

private fun createNullLiteralExpression(containingElement: UElement?) =
        object : ULiteralExpression, JvmDeclarationUElement {
            override val psi: PsiElement? = null
            override val uastParent: UElement? = containingElement
            override val value: Any? = null
            override val annotations: List<UAnnotation> = emptyList()
            override val javaPsi: PsiElement? = null
            override val sourcePsi: PsiElement? = null
        }

private fun createNotEqWithNullExpression(variable: UVariable, containingElement: UElement?) =
        object : UBinaryExpression, JvmDeclarationUElement {
            override val psi: PsiElement? = null
            override val uastParent: UElement? = containingElement
            override val leftOperand: UExpression by lz { createVariableReferenceExpression(variable, this) }
            override val rightOperand: UExpression by lz { createNullLiteralExpression(this) }
            override val operator: UastBinaryOperator = UastBinaryOperator.NOT_EQUALS
            override val operatorIdentifier: UIdentifier? = UIdentifier(null, this)
            override fun resolveOperator(): PsiMethod? = null
            override val annotations: List<UAnnotation> = emptyList()
            override val javaPsi: PsiElement? = null
            override val sourcePsi: PsiElement? = null
        }

private fun createElvisExpressions(
        left: KtExpression,
        right: KtExpression,
        containingElement: UElement?,
        psiParent: PsiElement): List<UExpression> {

    val declaration = KotlinUDeclarationsExpression(containingElement)
    val tempVariable = KotlinULocalVariable(UastKotlinPsiVariable.create(left, declaration, psiParent), left, declaration)
    declaration.declarations = listOf(tempVariable)

    val ifExpression = object : UIfExpression, JvmDeclarationUElement {
        override val psi: PsiElement? = null
        override val uastParent: UElement? = containingElement
        override val javaPsi: PsiElement? = null
        override val sourcePsi: PsiElement? = null
        override val condition: UExpression by lz { createNotEqWithNullExpression(tempVariable, this) }
        override val thenExpression: UExpression? by lz { createVariableReferenceExpression(tempVariable, this) }
        override val elseExpression: UExpression? by lz { KotlinConverter.convertExpression(right, this ) }
        override val isTernary: Boolean = false
        override val annotations: List<UAnnotation> = emptyList()
        override val ifIdentifier: UIdentifier = UIdentifier(null, this)
        override val elseIdentifier: UIdentifier? = UIdentifier(null, this)
    }

    return listOf(declaration, ifExpression)
}

fun createElvisExpression(elvisExpression: KtBinaryExpression, givenParent: UElement?): UExpression {
    val left = elvisExpression.left ?: return UastEmptyExpression
    val right = elvisExpression.right ?: return UastEmptyExpression

    return KotlinUElvisExpression(elvisExpression, left, right, givenParent)
}

class KotlinUElvisExpression(
    private val elvisExpression: KtBinaryExpression,
    private val left: KtExpression,
    private val right: KtExpression,
    givenParent: UElement?
) : KotlinAbstractUElement(givenParent), UExpressionList, KotlinEvaluatableUElement {

    override val javaPsi: PsiElement? = null
    override val sourcePsi: PsiElement? = elvisExpression
    override val psi: PsiElement? = sourcePsi
    override val kind = KotlinSpecialExpressionKinds.ELVIS
    override val annotations: List<UAnnotation> = emptyList()
    override val expressions: List<UExpression> by lz {
        createElvisExpressions(left, right, this, elvisExpression.parent)
    }

    val lhsDeclaration get() = (expressions[0] as UDeclarationsExpression).declarations.single()
    val rhsIfExpression get() = expressions[1] as UIfExpression

    override fun asRenderString(): String {
        return kind.name + " " +
               expressions.joinToString(separator = "\n", prefix = "{\n", postfix = "\n}") {
                   it.asRenderString().withMargin
               }
    }

    override fun getExpressionType(): PsiType? {
        val leftType = left.analyze()[BindingContext.EXPRESSION_TYPE_INFO, left]?.type ?: return null
        val rightType = right.analyze()[BindingContext.EXPRESSION_TYPE_INFO, right]?.type ?: return null

        return CommonSupertypes
            .commonSupertype(listOf(leftType, rightType))
            .toPsiType(this, elvisExpression, boxed = false)
    }
}
