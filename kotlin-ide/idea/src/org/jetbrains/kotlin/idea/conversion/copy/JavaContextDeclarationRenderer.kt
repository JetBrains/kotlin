/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.conversion.copy

import com.intellij.psi.util.parents
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptorWithAccessors
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.blockExpressionsOrSingle
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedMemberDescriptor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

data class ContextDeclarations(
    val localDeclarationsJavaStubs: String,
    val memberDeclarationsJavaStubs: String
)

class JavaContextDeclarationRenderer {
    private val KtElement.memberDeclarations
        get() = parents()
            .flatMap { declaration ->
                when (declaration) {
                    is KtClass -> declaration.resolveToDescriptorIfAny()
                        ?.unsubstitutedMemberScope
                        ?.getContributedDescriptors()
                        ?.asSequence()
                    is KtDeclarationContainer ->
                        declaration.declarations.mapNotNull { it.resolveToDescriptorIfAny() }.asSequence()
                    else -> null
                } ?: emptySequence()
            }.filter { member ->
                member !is DeserializedMemberDescriptor
                        && !member.name.isSpecial
                        && member.name.asString() != "dummy"
            }

    private val KtElement.localDeclarations
        get() = getParentOfType<KtDeclaration>(strict = false)
            ?.safeAs<KtFunction>()
            ?.bodyExpression
            ?.blockExpressionsOrSingle()
            ?.filterIsInstance<KtDeclaration>()
            ?.mapNotNull { it.resolveToDescriptorIfAny() }
            .orEmpty()

    fun render(contextElement: KtElement): ContextDeclarations =
        ContextDeclarations(
            contextElement.localDeclarations.render(),
            contextElement.memberDeclarations.render()
        )

    private fun Sequence<DeclarationDescriptor>.render() =
        buildString {
            for (member in this@render) {
                renderJavaDeclaration(member)
                appendln()
            }
        }


    private fun StringBuilder.renderJavaDeclaration(declaration: DeclarationDescriptor) {
        when (declaration) {
            is VariableDescriptorWithAccessors -> {
                renderType(declaration.type)
                append(' ')
                append(declaration.name.asString())
                append(" = null;")
            }
            is FunctionDescriptor -> {
                renderType(declaration.returnType)
                append(' ')
                append(declaration.name.asString())
                append('(')
                for ((i, parameter) in declaration.valueParameters.withIndex()) {
                    renderType(parameter.type)
                    append(' ')
                    append(parameter.name.asString())
                    if (i != declaration.valueParameters.lastIndex) {
                        append(", ")
                    }
                }
                append(") {}")
            }
        }
    }


    private fun StringBuilder.renderType(type: KotlinType?) {
        val fqName = type?.constructor?.declarationDescriptor?.fqNameUnsafe

        if (fqName != null) {
            renderFqName(fqName)
        } else {
            append("Object")
        }
        if (!type?.arguments.isNullOrEmpty()) {
            append("<")
            for (typeArgument in type!!.arguments) {
                if (typeArgument.isStarProjection) {
                    append("?")
                } else {
                    renderType(typeArgument.type)
                }
            }
            append(">")
        }
    }

    private fun StringBuilder.renderFqName(fqName: FqNameUnsafe) {
        val stringFqName = when (fqName) {
            KotlinBuiltIns.FQ_NAMES.unit -> "void"
            else -> JavaToKotlinClassMap.mapKotlinToJava(fqName)?.asSingleFqName() ?: fqName.asString()
        }
        append(stringFqName)
    }

}