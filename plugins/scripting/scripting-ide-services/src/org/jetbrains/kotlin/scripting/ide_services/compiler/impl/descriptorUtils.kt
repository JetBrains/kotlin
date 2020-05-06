/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.ide_services.compiler.impl

import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.descriptors.impl.TypeParameterDescriptorImpl
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.lexer.KtKeywordToken
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.renderer.ClassifierNamePolicy
import org.jetbrains.kotlin.renderer.ParameterNameRenderingPolicy
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.asFlexibleType
import org.jetbrains.kotlin.types.isFlexible
import kotlin.script.experimental.api.SourceCodeCompletionVariant

internal fun KtFile.getElementAt(cursorPos: Int): PsiElement? {
    var element: PsiElement? = findElementAt(cursorPos)
    while (element !is KtExpression && element != null) {
        element = element.parent
    }
    return element
}

internal const val DEFAULT_NUMBER_OF_CHARS = 40

internal class VisibilityFilter(
    private val inDescriptor: DeclarationDescriptor
) : (DeclarationDescriptor) -> Boolean {
    override fun invoke(descriptor: DeclarationDescriptor): Boolean {
        if (descriptor is TypeParameterDescriptor && !isTypeParameterVisible(descriptor)) return false

        if (descriptor is DeclarationDescriptorWithVisibility) {
            return try {
                descriptor.visibility.isVisible(null, descriptor, inDescriptor)
            } catch (e: IllegalStateException) {
                true
            }
        }

        return true
    }

    private fun isTypeParameterVisible(typeParameter: TypeParameterDescriptor): Boolean {
        val owner = typeParameter.containingDeclaration
        var parent: DeclarationDescriptor? = inDescriptor
        while (parent != null) {
            if (parent == owner) return true
            if (parent is ClassDescriptor && !parent.isInner) return false
            parent = parent.containingDeclaration
        }
        return true
    }
}

internal fun keywordsCompletionVariants(
    keywords: TokenSet,
    prefix: String
) = sequence {
    keywords.types.forEach {
        val token = (it as KtKeywordToken).value
        if (token.startsWith(prefix)) yield(
            SourceCodeCompletionVariant(
                token,
                token,
                "keyword",
                "keyword"
            )
        )
    }
}

internal val RENDERER =
    IdeDescriptorRenderers.SOURCE_CODE.withOptions {
        this.classifierNamePolicy =
            ClassifierNamePolicy.SHORT
        this.typeNormalizer =
            IdeDescriptorRenderers.APPROXIMATE_FLEXIBLE_TYPES
        this.parameterNameRenderingPolicy =
            ParameterNameRenderingPolicy.NONE
        this.renderDefaultAnnotationArguments = false
        this.typeNormalizer = lambda@{ kotlinType: KotlinType ->
            if (kotlinType.isFlexible()) {
                return@lambda kotlinType.asFlexibleType().upperBound
            }
            kotlinType
        }
    }

internal fun getIconFromDescriptor(descriptor: DeclarationDescriptor): String = when (descriptor) {
    is FunctionDescriptor -> "method"
    is PropertyDescriptor -> "property"
    is LocalVariableDescriptor -> "property"
    is ClassDescriptor -> "class"
    is PackageFragmentDescriptor -> "package"
    is PackageViewDescriptor -> "package"
    is ValueParameterDescriptor -> "genericValue"
    is TypeParameterDescriptorImpl -> "class"
    else -> ""
}

internal fun formatName(builder: String, symbols: Int = DEFAULT_NUMBER_OF_CHARS): String {
    return if (builder.length > symbols) {
        builder.substring(0, symbols) + "..."
    } else builder
}

internal data class DescriptorPresentation(
    val rawName: String,
    val presentableText: String,
    val tailText: String,
    val completionText: String,
    val typeText: String,
)
