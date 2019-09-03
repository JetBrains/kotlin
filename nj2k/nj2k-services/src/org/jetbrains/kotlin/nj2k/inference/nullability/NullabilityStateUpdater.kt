/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.inference.nullability

import org.jetbrains.kotlin.nj2k.inference.common.*
import org.jetbrains.kotlin.psi.KtNullableType
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtTypeElement
import org.jetbrains.kotlin.psi.psiUtil.isAncestor

class NullabilityStateUpdater : StateUpdater() {
    override fun updateStates(inferenceContext: InferenceContext) {
        if (inferenceContext.typeVariables.isEmpty()) return

        val deepComparator = Comparator<TypeElementBasedTypeVariable> { o1, o2 ->
            if (o1.typeElement.typeElement.isAncestor(o2.typeElement.typeElement)) 1 else -1
        }
        for (typeVariable in inferenceContext
            .typeVariables
            .filterIsInstance<TypeElementBasedTypeVariable>()
            .sortedWith(deepComparator)
        ) {
            when (typeVariable.state) {
                State.LOWER -> typeVariable.changeState(toNullable = false)
                State.UPPER -> typeVariable.changeState(toNullable = true)
                State.UNKNOWN -> {
                    if (typeVariable.typeElement is TypeParameterElementData) {
                        typeVariable.changeState(toNullable = false)
                    } else {
                        typeVariable.changeState(toNullable = true)
                    }
                }
            }
        }
    }

    private fun TypeElementBasedTypeVariable.changeState(toNullable: Boolean) {
        changeState(typeElement.typeElement, toNullable)
    }

    companion object {
        fun changeState(typeElement: KtTypeElement, toNullable: Boolean) {
            val factory = KtPsiFactory(typeElement)
            if (typeElement is KtNullableType && !toNullable) {
                typeElement.replace(factory.createType(typeElement.innerType?.text ?: return).typeElement ?: return)
            }
            if (typeElement !is KtNullableType && toNullable) {
                typeElement.replace(factory.createType("${typeElement.text}?").typeElement ?: return)
            }
        }
    }

}