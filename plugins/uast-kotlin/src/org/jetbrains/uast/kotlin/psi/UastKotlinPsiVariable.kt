package org.jetbrains.uast.kotlin.psi

import com.intellij.psi.*
import com.intellij.psi.impl.light.LightTypeElement
import org.jetbrains.kotlin.asJava.elements.LightVariableBuilder
import org.jetbrains.kotlin.builtins.createFunctionType
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.utils.ifEmpty
import org.jetbrains.uast.UDeclaration
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UastErrorType
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.kotlin.analyze
import org.jetbrains.uast.kotlin.lz
import org.jetbrains.uast.kotlin.orAnonymous
import org.jetbrains.uast.kotlin.toPsiType

class UastKotlinPsiVariable(
        manager: PsiManager,
        name: String,
        type: PsiType,
        val ktInitializer: KtExpression?,
        val psiParent: PsiElement?,
        val containingElement: UElement,
        val ktElement: KtElement
) : LightVariableBuilder(manager, name, type, KotlinLanguage.INSTANCE), PsiLocalVariable {

    private val psiTypeElement: PsiTypeElement by lz {
        LightTypeElement(manager, type)
    }

    private val psiInitializer: PsiExpression? by lz {
        ktInitializer?.let { KotlinUastPsiExpression(it, containingElement) }
    }

    override fun getParent() = psiParent

    override fun hasInitializer() = ktInitializer != null

    override fun getInitializer(): PsiExpression? = psiInitializer

    override fun getTypeElement() = psiTypeElement

    override fun setInitializer(initializer: PsiExpression?) = throw NotImplementedError()

    override fun getContainingFile(): PsiFile? = ktElement.containingFile

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other::class.java != this::class.java) return false
        return ktElement == (other as? UastKotlinPsiVariable)?.ktElement
    }

    override fun hashCode() = ktElement.hashCode()

    companion object {
        fun create(
                declaration: KtVariableDeclaration, 
                parent: PsiElement?, 
                containingElement: UElement, 
                initializer: KtExpression? = null
        ): PsiLocalVariable {
            val psiParent = containingElement.getParentOfType<UDeclaration>()?.psi ?: parent
            return UastKotlinPsiVariable(
                    declaration.manager,
                    declaration.name.orAnonymous("unnamed"),
                    declaration.typeReference.toPsiType(containingElement),
                    initializer ?: declaration.initializer,
                    psiParent,
                    containingElement,
                    declaration)
        }
        
        fun create(declaration: KtDestructuringDeclaration, containingElement: UElement): PsiLocalVariable {
            val psiParent = containingElement.getParentOfType<UDeclaration>()?.psi ?: declaration.parent
            return UastKotlinPsiVariable(
                    declaration.manager,
                    "var" + Integer.toHexString(declaration.getHashCode()),
                    declaration.initializer?.getType(containingElement) ?: UastErrorType,
                    declaration.initializer,
                    psiParent,
                    containingElement,
                    declaration)
        }

        fun create(initializer: KtExpression, containingElement: UElement, parent: PsiElement): PsiLocalVariable {
            val psiParent = containingElement.getParentOfType<UDeclaration>()?.psi ?: parent
            return UastKotlinPsiVariable(
                    initializer.manager,
                    "var" + Integer.toHexString(initializer.getHashCode()),
                    initializer.getType(containingElement) ?: UastErrorType,
                    initializer,
                    psiParent,
                    containingElement,
                    initializer)
        }

        fun create(name: String, localFunction: KtFunction, containingElement: UElement): PsiLocalVariable {
            val psiParent = containingElement.getParentOfType<UDeclaration>()?.psi ?: localFunction.parent
            return UastKotlinPsiVariable(
                    localFunction.manager,
                    name,
                    localFunction.getFunctionType(containingElement) ?: UastErrorType,
                    localFunction,
                    psiParent,
                    containingElement,
                    localFunction)
        }
    }
}

private class KotlinUastPsiExpression(val ktExpression: KtExpression, val parent: UElement) : PsiElement by ktExpression, PsiExpression {
    override fun getType(): PsiType? = ktExpression.getType(parent)
}

private fun KtFunction.getFunctionType(parent: UElement): PsiType? {
    val descriptor = analyze()[BindingContext.FUNCTION, this] ?: return null
    val returnType = descriptor.returnType ?: return null

    return createFunctionType(
            descriptor.builtIns,
            descriptor.annotations,
            descriptor.extensionReceiverParameter?.type,
            descriptor.valueParameters.map { it.type },
            descriptor.valueParameters.map { it.name },
            returnType
    ).toPsiType(parent, this, boxed = false)
}

private fun KtExpression.getType(parent: UElement): PsiType? =
        analyze()[BindingContext.EXPRESSION_TYPE_INFO, this]?.type?.toPsiType(parent, this, boxed = false)

private fun PsiElement.getHashCode(): Int {
    var result = 42
    result = 41 * result + containingFile.name.hashCode()
    result = 41 * result + startOffset
    result = 41 * result + text.hashCode()
    return result
}