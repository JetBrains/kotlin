package org.jetbrains.uast.kotlin.psi

import com.intellij.psi.*
import com.intellij.psi.impl.light.LightTypeElement
import org.jetbrains.kotlin.asJava.elements.LightVariableBuilder
import org.jetbrains.kotlin.builtins.createFunctionType
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.types.isError
import org.jetbrains.uast.UDeclaration
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UastErrorType
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.kotlin.analyze
import org.jetbrains.uast.kotlin.lz
import org.jetbrains.uast.kotlin.orAnonymous
import org.jetbrains.uast.kotlin.toPsiType

class UastKotlinPsiVariable private constructor(
        manager: PsiManager,
        name: String,
        typeProducer: () -> PsiType,
        val ktInitializer: KtExpression?,
        val psiParent: PsiElement?,
        val containingElement: UElement,
        val ktElement: KtElement
) : LightVariableBuilder(
        manager,
        name,
        UastErrorType, // Type is calculated lazily
        KotlinLanguage.INSTANCE
), PsiLocalVariable {

    private val psiType: PsiType by lz(typeProducer)

    private val psiTypeElement: PsiTypeElement by lz {
        LightTypeElement(manager, psiType)
    }

    private val psiInitializer: PsiExpression? by lz {
        ktInitializer?.let { KotlinUastPsiExpression(it, containingElement) }
    }

    override fun getType(): PsiType = psiType

    override fun getText(): String = ktElement.text

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
            val initializerExpression = initializer ?: declaration.initializer
            return UastKotlinPsiVariable(
                    manager = declaration.manager,
                    name = declaration.name.orAnonymous("<unnamed>"),
                    typeProducer = { declaration.getType(containingElement) ?: UastErrorType },
                    ktInitializer = initializerExpression,
                    psiParent = psiParent,
                    containingElement = containingElement,
                    ktElement = declaration)
        }
        
        fun create(declaration: KtDestructuringDeclaration, containingElement: UElement): PsiLocalVariable {
            val psiParent = containingElement.getParentOfType<UDeclaration>()?.psi ?: declaration.parent
            return UastKotlinPsiVariable(
                    manager = declaration.manager,
                    name = "var" + Integer.toHexString(declaration.getHashCode()),
                    typeProducer = { declaration.getType(containingElement) ?: UastErrorType },
                    ktInitializer = declaration.initializer,
                    psiParent = psiParent,
                    containingElement = containingElement,
                    ktElement = declaration)
        }

        fun create(initializer: KtExpression, containingElement: UElement, parent: PsiElement): PsiLocalVariable {
            val psiParent = containingElement.getParentOfType<UDeclaration>()?.psi ?: parent
            return UastKotlinPsiVariable(
                    manager = initializer.manager,
                    name = "var" + Integer.toHexString(initializer.getHashCode()),
                    typeProducer = { initializer.getType(containingElement) ?: UastErrorType },
                    ktInitializer = initializer,
                    psiParent = psiParent,
                    containingElement = containingElement,
                    ktElement = initializer)
        }

        fun create(name: String, localFunction: KtFunction, containingElement: UElement): PsiLocalVariable {
            val psiParent = containingElement.getParentOfType<UDeclaration>()?.psi ?: localFunction.parent
            return UastKotlinPsiVariable(
                    manager = localFunction.manager,
                    name = name,
                    typeProducer = { localFunction.getFunctionType(containingElement) ?: UastErrorType },
                    ktInitializer = localFunction,
                    psiParent = psiParent,
                    containingElement = containingElement,
                    ktElement = localFunction)
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
            builtIns = descriptor.builtIns,
            annotations = descriptor.annotations,
            receiverType = descriptor.extensionReceiverParameter?.type,
            parameterTypes = descriptor.valueParameters.map { it.type },
            parameterNames = descriptor.valueParameters.map { it.name },
            returnType = returnType
    ).toPsiType(parent, this, boxed = false)
}

private fun KtExpression.getType(parent: UElement): PsiType? =
        analyze()[BindingContext.EXPRESSION_TYPE_INFO, this]?.type?.toPsiType(parent, this, boxed = false)

private fun KtDeclaration.getType(parent: UElement): PsiType? {
    return (analyze()[BindingContext.DECLARATION_TO_DESCRIPTOR, this] as? CallableDescriptor)
                   ?.returnType
                   ?.takeIf { !it.isError }
                   ?.toPsiType(parent, this, false)
}

private fun PsiElement.getHashCode(): Int {
    var result = 42
    result = 41 * result + containingFile.name.hashCode()
    result = 41 * result + startOffset
    result = 41 * result + text.hashCode()
    return result
}