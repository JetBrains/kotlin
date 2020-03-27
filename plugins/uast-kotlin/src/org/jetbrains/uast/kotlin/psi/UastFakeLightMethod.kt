/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin.psi

import com.intellij.psi.*
import com.intellij.psi.impl.light.LightMethodBuilder
import com.intellij.psi.impl.light.LightModifierList
import com.intellij.psi.impl.light.LightParameterListBuilder
import com.intellij.psi.impl.light.LightTypeParameterBuilder
import org.jetbrains.kotlin.asJava.elements.KotlinLightTypeParameterListBuilder
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.uast.UastErrorType
import org.jetbrains.uast.kotlin.analyze
import org.jetbrains.uast.kotlin.getType
import org.jetbrains.uast.kotlin.toPsiType

internal class UastFakeLightMethod(internal val original: KtFunction, containingClass: PsiClass) : LightMethodBuilder(
    original.manager, original.language, original.name ?: "<no name provided>",
    LightParameterListBuilder(original.manager, original.language),
    LightModifierList(original.manager)
) {

    init {
        this.containingClass = containingClass
        if (original.safeAs<KtNamedFunction>()?.isTopLevel == true) {
            addModifier(PsiModifier.STATIC)
        }
    }

    private val _buildTypeParameterList by lazy {
        KotlinLightTypeParameterListBuilder(this).also { paramList ->
            for ((i, p) in original.typeParameters.withIndex()) {
                paramList.addParameter(
                    LightTypeParameterBuilder(
                        p.name ?: "__no_name__",
                        this,
                        i
                    )
                )
            }
        }
    }

    override fun getTypeParameterList(): PsiTypeParameterList? = _buildTypeParameterList

    private val paramsList: PsiParameterList by lazy {
        object : LightParameterListBuilder(original.manager, original.language) {
            override fun getParent(): PsiElement = this@UastFakeLightMethod
            override fun getContainingFile(): PsiFile = parent.containingFile

            init {
                val parameterList = this

                original.receiverTypeReference?.let { receiver ->
                    this.addParameter(
                        UastKotlinPsiParameterBase(
                            "\$this\$${original.name}",
                            receiver.getType()
                                ?.toPsiType(this@UastFakeLightMethod, original, false)
                                ?: UastErrorType,
                            parameterList, receiver
                        )
                    )
                }

                for ((i, p) in original.valueParameters.withIndex()) {
                    this.addParameter(
                        UastKotlinPsiParameter(
                            p.name ?: "p$i",
                            p.typeReference?.getType()
                                ?.toPsiType(this@UastFakeLightMethod, original, false)
                                ?: UastErrorType,
                            parameterList, original.language, p.isVarArg, p.defaultValue, p
                        )
                    )
                }
            }
        }
    }

    override fun getParameterList(): PsiParameterList = paramsList

    override fun getReturnType(): PsiType? {
        val context = original.analyze()
        val descriptor = context.get(BindingContext.DECLARATION_TO_DESCRIPTOR, original).safeAs<CallableDescriptor>() ?: return null
        return descriptor.returnType?.toPsiType(this, original, false)
    }

    override fun getParent(): PsiElement? = containingClass

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UastFakeLightMethod

        if (original != other.original) return false

        return true
    }

    override fun hashCode(): Int = original.hashCode()
}