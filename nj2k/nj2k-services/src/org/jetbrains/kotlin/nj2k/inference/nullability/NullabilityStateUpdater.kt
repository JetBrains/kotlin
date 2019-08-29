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
    override fun TypeElementBasedTypeVariable.updateState() = when (state) {
        State.LOWER -> changeState(toNullable = false)
        State.UPPER -> changeState(toNullable = true)
        State.UNKNOWN -> {
            if (typeElement is TypeParameterElementData) {
                changeState(toNullable = false)
            } else {
                changeState(toNullable = true)
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