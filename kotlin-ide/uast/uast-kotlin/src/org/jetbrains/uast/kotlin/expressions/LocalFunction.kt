package org.jetbrains.uast.kotlin.expressions

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiType
import com.intellij.psi.PsiVariable
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.uast.*
import org.jetbrains.uast.kotlin.*
import org.jetbrains.uast.kotlin.declarations.wrapExpressionBody
import org.jetbrains.uast.kotlin.psi.UastKotlinPsiParameter
import org.jetbrains.uast.kotlin.psi.UastKotlinPsiVariable


internal class KotlinLocalFunctionUVariable(
    val function: KtFunction,
    override val javaPsi: PsiVariable,
    givenParent: UElement?
) : KotlinAbstractUElement(givenParent), UVariableExPlaceHolder, PsiVariable by javaPsi {

    override val psi get() = javaPsi
    override val sourcePsi: PsiElement? = (javaPsi as? UastKotlinPsiVariable?)?.ktElement ?: javaPsi

    override val uastInitializer: UExpression? by lz {
        createLocalFunctionLambdaExpression(function, this)
    }
    override val typeReference: UTypeReferenceExpression? = null
    override val uastAnchor: UElement? = null
    override val annotations: List<UAnnotation> = emptyList()
    override fun getOriginalElement(): PsiElement {
        return psi.originalElement
    }

    override fun getInitializer(): PsiExpression? {
        return psi.initializer
    }
}


internal class KotlinLocalFunctionULambdaExpression(
        override val sourcePsi: KtFunction,
        givenParent: UElement?
): KotlinAbstractUExpression(givenParent), ULambdaExpression {
    override val functionalInterfaceType: PsiType? = null

    override val body by lz {
        sourcePsi.bodyExpression?.let { wrapExpressionBody(this, it) } ?: UastEmptyExpression(this)
    }

    override val valueParameters by lz {
        sourcePsi.valueParameters.mapIndexed { i, p ->
            KotlinUParameter(UastKotlinPsiParameter.create(p, sourcePsi, this, i), p, this)
        }
    }

    override fun asRenderString(): String {
        val renderedValueParameters = valueParameters.joinToString(prefix = "(", postfix = ")",
                transform = KotlinUParameter::asRenderString)
        val expressions = (body as? UBlockExpression)?.expressions?.joinToString("\n") {
            it.asRenderString().withMargin
        } ?: body.asRenderString()
        return "fun $renderedValueParameters {\n${expressions.withMargin}\n}"
    }
}


fun createLocalFunctionDeclaration(function: KtFunction, parent: UElement?): UDeclarationsExpression {
    return KotlinUDeclarationsExpression(null, parent, function).apply {
        val functionVariable = UastKotlinPsiVariable.create(function.name.orAnonymous(), function, this)
        declarations = listOf(KotlinLocalFunctionUVariable(function, functionVariable, this))
    }
}

fun createLocalFunctionLambdaExpression(function: KtFunction, parent: UElement?): ULambdaExpression =
        KotlinLocalFunctionULambdaExpression(function, parent)
