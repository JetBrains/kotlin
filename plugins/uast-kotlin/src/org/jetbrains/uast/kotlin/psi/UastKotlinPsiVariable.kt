package org.jetbrains.uast.kotlin.psi

import com.intellij.lang.Language
import com.intellij.psi.*
import org.jetbrains.kotlin.asJava.elements.LightVariableBuilder
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtVariableDeclaration
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.uast.UDeclaration
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UastErrorType
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.kotlin.analyze
import org.jetbrains.uast.kotlin.orAnonymous
import org.jetbrains.uast.kotlin.toPsiType

class UastKotlinPsiVariable(
        manager: PsiManager,
        name: String,
        type: PsiType,
        language: Language,
        val ktInitializer: KtExpression?,
        val psiParent: PsiElement?,
        val containingElement: UElement,
        val ktElement: KtElement
) : LightVariableBuilder(manager, name, type, language), PsiLocalVariable {
    override fun getParent() = psiParent

    override fun hasInitializer() = ktInitializer != null
    override fun getInitializer(): PsiExpression? = ktInitializer?.let { KotlinUastPsiExpression(it, containingElement) }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false
        return ktElement == (other as? UastKotlinPsiVariable)?.ktElement
    }

    override fun getTypeElement() = throw NotImplementedError()
    override fun setInitializer(initializer: PsiExpression?) = throw NotImplementedError()

    override fun getContainingFile(): PsiFile? = ktElement.containingFile

    override fun hashCode() = ktElement.hashCode()

    companion object {
        fun create(
                declaration: KtVariableDeclaration, 
                parent: PsiElement?, 
                containingElement: UElement, 
                initializer: KtExpression? = null
        ): PsiVariable {
            val psiParent = containingElement.getParentOfType<UDeclaration>()?.psi ?: parent
            return UastKotlinPsiVariable(
                    declaration.manager,
                    declaration.name.orAnonymous("unnamed"),
                    declaration.typeReference.toPsiType(containingElement),
                    KotlinLanguage.INSTANCE,
                    initializer ?: declaration.initializer,
                    psiParent,
                    containingElement,
                    declaration)
        }
        
        fun create(declaration: KtDestructuringDeclaration, containingElement: UElement): PsiVariable {
            val psiParent = containingElement.getParentOfType<UDeclaration>()?.psi ?: declaration.parent
            return UastKotlinPsiVariable(
                    declaration.manager,
                    "var" + Integer.toHexString(declaration.hashCode()),
                    UastErrorType, //TODO,
                    KotlinLanguage.INSTANCE,
                    declaration.initializer,
                    psiParent,
                    containingElement,
                    declaration)
        }
    }
}

private class KotlinUastPsiExpression(val ktExpression: KtExpression, val parent: UElement) : PsiElement by ktExpression, PsiExpression {
    override fun getType(): PsiType? {
        val ktType = ktExpression.analyze()[BindingContext.EXPRESSION_TYPE_INFO, ktExpression]?.type ?: return null
        return ktType.toPsiType(parent, ktExpression, boxed = false)
    }
}