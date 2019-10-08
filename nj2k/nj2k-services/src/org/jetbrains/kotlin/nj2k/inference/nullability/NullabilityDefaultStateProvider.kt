/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.inference.nullability

import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.idea.refactoring.isAbstract
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.nj2k.inference.common.*
import org.jetbrains.kotlin.psi.psiUtil.isPrivate

class NullabilityDefaultStateProvider : DefaultStateProvider() {
    override fun defaultStateFor(typeVariable: TypeVariable): State {
        if (typeVariable is TypeElementBasedTypeVariable
            && typeVariable.typeElement.type.constructor.declarationDescriptor is TypeParameterDescriptor
        ) {
            return State.LOWER
        }
        return when (val owner = typeVariable.owner) {
            is FunctionParameter -> if (owner.owner.isPrivate()) State.LOWER else State.UPPER
            is FunctionReturnType ->
                if (owner.function.isAbstract()
                    || owner.function.hasModifier(KtTokens.OPEN_KEYWORD)
                    || owner.function.bodyExpression == null
                ) State.UPPER else State.LOWER
            is Property -> if (owner.property.isPrivate()
                || !owner.property.isVar
                || !owner.property.isLocal
            ) State.LOWER else State.UPPER
            is TypeArgument -> State.LOWER
            OtherTarget -> State.UPPER
        }
    }
}