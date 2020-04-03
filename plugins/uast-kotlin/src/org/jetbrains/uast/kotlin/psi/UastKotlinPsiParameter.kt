package org.jetbrains.uast.kotlin.psi

import com.intellij.lang.Language
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiType
import com.intellij.psi.impl.light.LightParameter
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.uast.UDeclaration
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UastErrorType
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.kotlin.analyze
import org.jetbrains.uast.kotlin.toPsiType

class UastKotlinPsiParameter(
    name: String,
    type: PsiType,
    parent: PsiElement,
    language: Language,
    isVarArgs: Boolean,
    ktDefaultValue: KtExpression?,
    ktParameter: KtParameter
) : UastKotlinPsiParameterBase<KtParameter>(name, type, parent, ktParameter, language, isVarArgs, ktDefaultValue) {
    companion object {
        fun create(parameter: KtParameter, parent: PsiElement, containingElement: UElement, index: Int): PsiParameter {
            val psiParent = containingElement.getParentOfType<UDeclaration>()?.javaPsi ?: parent
            return UastKotlinPsiParameter(
                    parameter.name ?: "p$index",
                    (parameter.analyze()[BindingContext.DECLARATION_TO_DESCRIPTOR, parameter] as? VariableDescriptor)
                            ?.type?.toPsiType(containingElement, parameter, boxed = false) ?: UastErrorType,
                    psiParent,
                    KotlinLanguage.INSTANCE,
                    parameter.isVarArg,
                    parameter.defaultValue,
                    parameter)
        }
    }

    val ktParameter: KtParameter get() = ktOrigin

}

open class UastKotlinPsiParameterBase<T : KtElement>(
    name: String,
    type: PsiType,
    private val parent: PsiElement,
    val ktOrigin: T,
    language: Language = ktOrigin.language,
    isVarArgs: Boolean = false,
    val ktDefaultValue: KtExpression? = null,
) : LightParameter(name, type, parent, language, isVarArgs) {

    override fun getParent(): PsiElement = parent

    override fun getContainingFile(): PsiFile? = ktOrigin.containingFile

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other::class.java != this::class.java) return false
        return ktOrigin == (other as? UastKotlinPsiParameterBase<*>)?.ktOrigin
    }

    override fun hashCode(): Int = ktOrigin.hashCode()
}