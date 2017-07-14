package org.jetbrains.uast.kotlin.expressions

import com.intellij.psi.PsiType
import com.intellij.psi.PsiVariable
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.uast.*
import org.jetbrains.uast.kotlin.*
import org.jetbrains.uast.kotlin.psi.UastKotlinPsiParameter
import org.jetbrains.uast.kotlin.psi.UastKotlinPsiVariable


private class KotlinLocalFunctionUVariable(
        val function: KtFunction,
        override val psi: PsiVariable,
        override val uastParent: UElement?
) : UVariable, PsiVariable by psi {
    override val uastInitializer: UExpression? by lz {
        createLocalFunctionLambdaExpression(function, this)
    }
    override val typeReference: UTypeReferenceExpression? = null
    override val uastAnchor: UElement? = null
    override val annotations: List<UAnnotation> = emptyList()

    override fun equals(other: Any?) = other is KotlinLocalFunctionUVariable && psi == other.psi
    override fun hashCode() = psi.hashCode()
}


private class KotlinLocalFunctionULambdaExpression(
        override val psi: KtFunction,
        override val uastParent: UElement?
): KotlinAbstractUExpression(), ULambdaExpression {
    val functionalInterfaceType: PsiType?
        get() = null

    override val body by lz { KotlinConverter.convertOrEmpty(psi.bodyExpression, this) }

    override val valueParameters by lz {
        psi.valueParameters.mapIndexed { i, p ->
            KotlinUParameter(UastKotlinPsiParameter.create(p, psi, this, i), this)
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
    return KotlinUDeclarationsExpression(parent).apply {
        val functionVariable = UastKotlinPsiVariable.create(function.name.orAnonymous(), function, this)
        declarations = listOf(KotlinLocalFunctionUVariable(function, functionVariable, this))
    }
}

fun createLocalFunctionLambdaExpression(function: KtFunction, parent: UElement?): ULambdaExpression =
        KotlinLocalFunctionULambdaExpression(function, parent)
