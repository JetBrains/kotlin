/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.inference.common

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtTypeElement
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

class DebugPrinter(private val inferenceContext: InferenceContext) {
    private val namer = Namer(inferenceContext)

    val TypeVariable.name: String
        get() = namer.name(this)

    private fun ClassReference.asString() = when (this) {
        is DescriptorClassReference -> descriptor.name.toString()
        is TypeParameterReference -> descriptor.name.toString()
        is NoClassReference -> "NoClassRef"
    }

    fun BoundTypeLabel.asString(): String = when (this) {
        is TypeVariableLabel -> typeVariable.name + "@" + typeVariable.classReference.asString()
        is TypeParameterLabel -> typeParameter.name.asString()
        is GenericLabel -> classReference.asString()
        StarProjectionLabel -> "*"
        NullLiteralLabel -> "NULL"
        LiteralLabel -> "LIT"
    }

    fun State.asString() = when (this) {
        State.LOWER -> "L"
        State.UPPER -> "U"
        State.UNKNOWN -> "?"
    }

    fun BoundType.asString(): String = buildString {
        append(label.asString())
        if (typeParameters.isNotEmpty()) {
            typeParameters.joinTo(this, ", ", "<", ">") { it.boundType.asString() }
        }
        if (this@asString is WithForcedStateBoundType) {
            append("!!")
            append(forcedState.asString())
        }
    }

    fun Constraint.asString() = when (this) {
        is EqualsConstraint -> "${left.asString()} := ${right.asString()}"
        is SubtypeConstraint -> "${subtype.asString()} <: ${supertype.asString()}"
    } + " due to '$priority'"


    private fun ConstraintBound.asString(): String = when (this) {
        is LiteralBound -> state.toString()
        is TypeVariableBound -> typeVariable.name
    }

    fun PsiElement.addTypeVariablesNames() {
        val factory = KtPsiFactory(this)
        for (typeElement in collectDescendantsOfType<KtTypeElement>()) {
            val typeVariableName = this@DebugPrinter.inferenceContext.typeElementToTypeVariable[typeElement]?.name ?: continue
            val comment = factory.createComment("/*$typeVariableName@*/")
            typeElement.parent.addBefore(comment, typeElement)
        }
    }
}


private class Namer(inferenceContext: InferenceContext) {
    val names = inferenceContext.typeVariables.mapIndexed { index, typeVariable ->
        typeVariable to "T$index"
    }.toMap()


    fun name(typeVariable: TypeVariable): String =
        names.getValue(typeVariable)
}