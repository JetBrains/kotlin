/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.nullabilityAnalysis

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtTypeElement
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

internal class Printer(private val analysisContext: AnalysisContext) {
    private val namer = Namer(analysisContext)

    internal val TypeVariable.name: String
        get() = namer.name(this)

    private fun ClassReference.asString() =
        when (this) {
            is KtClassReference -> klass.name
            is JavaClassReference -> klass.name
            is DescriptorClassReference -> descriptor.name.toString()
            is TypeParameterClassReference -> typeParameter.name
            is LiteralClassReference -> "LITERAL"
            is UnknownClassReference -> text
            else -> error(this::class.toString())
        }

    fun BoundType.asString(): String =
        buildString {
            if (this@asString is TypeVariableBoundType) {
                append(this@asString.typeVariable.name)
                append("@")
            }
            append(classReference.asString())
            if (typeParameters.isNotEmpty()) {
                typeParameters.joinTo(this, ", ", "<", ">") { it.boundType.asString() }
            }
        }

    private fun Constraint.asString() =
        when (this) {
            is EqualConstraint -> "${leftBound.asString()} := ${rightBound.asString()}"
            is SubtypeConstraint -> "${lowerBound.asString()} <: ${upperBound.asString()}"
            else -> error("Unknown constraint ${this::class.qualifiedName}")
        } + ", because of '$cameFrom'"


    private fun ConstraintBound.asString(): String =
        when (this) {
            is LiteralBound -> nullability.toString()
            is TypeVariableBound -> typeVariable.name
            else -> error("Unknown constraint bound ${this::class.qualifiedName}")
        }


    internal fun PsiElement.addTypeVariablesNames() {
        val factory = KtPsiFactory(this)
        for (typeElement in collectDescendantsOfType<KtTypeElement>()) {
            val typeVariableName = this@Printer.analysisContext.typeElementToTypeVariable[typeElement]?.name ?: continue
            val comment = factory.createComment("/*$typeVariableName@*/")
            typeElement.parent.addBefore(comment, typeElement)
        }
    }

    internal fun List<Constraint>.listConstrains() =
        joinToString(separator = "\n") { it.asString() }
}


private class Namer(analysisContext: AnalysisContext) {
    val names = analysisContext.typeElementToTypeVariable.values.mapIndexed { index, typeVariable ->
        typeVariable to "T$index"
    }.toMap()

    fun name(typeVariable: TypeVariable): String =
        names.getValue(typeVariable)
}