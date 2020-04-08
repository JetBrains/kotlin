/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.inference.common

import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.idea.refactoring.isAbstract
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTypeElement
import org.jetbrains.kotlin.psi.psiUtil.isPrivate
import org.jetbrains.kotlin.types.KotlinType

sealed class TypeVariable {
    abstract val classReference: ClassReference
    abstract val typeParameters: List<TypeParameter>
    abstract val owner: TypeVariableOwner
    abstract var state: State
}

sealed class TypeVariableOwner

class FunctionParameter(val owner: KtFunction) : TypeVariableOwner()
class FunctionReturnType(val function: KtFunction) : TypeVariableOwner()
class Property(val property: KtProperty) : TypeVariableOwner()
object TypeArgument : TypeVariableOwner()
object OtherTarget : TypeVariableOwner()


sealed class TypeElementData {
    abstract val typeElement: KtTypeElement
    abstract val type: KotlinType
}

data class TypeElementDataImpl(
    override val typeElement: KtTypeElement,
    override val type: KotlinType
) : TypeElementData()

data class TypeParameterElementData(
    override val typeElement: KtTypeElement,
    override val type: KotlinType,
    val typeParameterDescriptor: TypeParameterDescriptor
) : TypeElementData()

class TypeElementBasedTypeVariable(
    override val classReference: ClassReference,
    override val typeParameters: List<TypeParameter>,
    val typeElement: TypeElementData,
    override val owner: TypeVariableOwner,
    override var state: State
) : TypeVariable()

class TypeBasedTypeVariable(
    override val classReference: ClassReference,
    override val typeParameters: List<TypeParameter>,
    val type: KotlinType,
    override var state: State
) : TypeVariable() {
    override val owner = OtherTarget
}

val TypeVariable.isFixed: Boolean
    get() = state != State.UNKNOWN

fun TypeVariable.setStateIfNotFixed(newState: State) {
    if (!isFixed) {
        state = newState
    }
}

