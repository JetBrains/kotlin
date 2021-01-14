package org.jetbrains.uast.kotlin.psi

import com.intellij.lang.Language
import com.intellij.psi.*
import com.intellij.psi.impl.light.LightParameter
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.components.isVararg
import org.jetbrains.uast.UDeclaration
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UastErrorType
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.kotlin.analyze
import org.jetbrains.uast.kotlin.toPsiType

internal class UastDescriptorLightParameter(
    name: String,
    type: PsiType,
    parent: PsiElement,
    ktParameter: ValueParameterDescriptor,
    language: Language = parent.language,
) : UastDescriptorLightParameterBase<ValueParameterDescriptor>(name, type, parent, ktParameter, language)

internal open class UastDescriptorLightParameterBase<T : ParameterDescriptor>(
    name: String,
    type: PsiType,
    private val parent: PsiElement,
    val ktOrigin: T,
    language: Language = parent.language,
) : LightParameter(name, type, parent, language, ktOrigin.isVararg) {

    override fun getParent(): PsiElement = parent

    override fun getContainingFile(): PsiFile? = parent.containingFile

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other::class.java != this::class.java) return false
        return ktOrigin == (other as? UastDescriptorLightParameterBase<*>)?.ktOrigin
    }

    override fun hashCode(): Int = ktOrigin.hashCode()
}
