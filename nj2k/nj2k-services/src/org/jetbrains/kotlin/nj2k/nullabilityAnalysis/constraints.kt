/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.nullabilityAnalysis

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiTypeParameter
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addToStdlib.safeAs


internal interface TypeVariableOwner {
    val target: KtElement
}

internal interface SingleTypeVariableOwner : TypeVariableOwner {
    val typeVariable: TypeVariable
}

internal interface MultipleTypeVariablesOwner : TypeVariableOwner {
    val typeVariables: List<TypeVariable>
}

internal val TypeVariableOwner.allTypeVariables
    get() = when (this) {
        is SingleTypeVariableOwner -> listOf(typeVariable)
        is MultipleTypeVariablesOwner -> typeVariables
        else -> error("TypeVariableOwner should be either SingleTypeVariableOwner or MultipleTypeVariablesOwner")
    }


internal interface DeclarationTypeVariableOwner : SingleTypeVariableOwner {
    override val target: KtCallableDeclaration
}

internal data class PropertyTarget(
    override val target: KtProperty,
    override val typeVariable: TypeVariable
) : DeclarationTypeVariableOwner

internal data class FunctionTarget(
    override val target: KtNamedFunction,
    override val typeVariable: TypeVariable
) : DeclarationTypeVariableOwner

internal data class LambdaTarget(
    override val target: KtFunctionLiteral,
    override val typeVariable: TypeVariable
) : DeclarationTypeVariableOwner

internal data class ParameterTarget(
    override val target: KtParameter,
    override val typeVariable: TypeVariable
) : DeclarationTypeVariableOwner

internal data class TypeCastTarget(
    override val target: KtBinaryExpressionWithTypeRHS,
    override val typeVariable: TypeVariable
) : SingleTypeVariableOwner

internal data class FunctionCallTypeArgumentTarget(
    override val target: KtCallExpression,
    override val typeVariables: List<TypeVariable>
) : MultipleTypeVariablesOwner


internal interface ClassReference
internal data class KtClassReference(val klass: KtClassOrObject) : ClassReference
internal data class DescriptorClassReference(val descriptor: ClassDescriptor) : ClassReference
internal data class JavaClassReference(val klass: PsiClass) : ClassReference
internal data class TypeParameterClassReference(val typeParameter: KtTypeParameter) : ClassReference
internal data class UnknownClassReference(val text: String) : ClassReference
internal object LiteralClassReference : ClassReference


internal interface BoundType {
    val classReference: ClassReference
    val typeParameters: List<BoundTypeTypeParameter>
    val forcedNullabilityTo: Nullability?
}

internal class TypeVariableBoundType(
    val typeVariable: TypeVariable,
    override val forcedNullabilityTo: Nullability? = null
) : BoundType {
    override val classReference: ClassReference = typeVariable.classReference
    override val typeParameters: List<BoundTypeTypeParameter> =
        typeVariable.typeParameters.map { typeParameter ->
            val boundType =
                if (typeParameter is TypeVariableTypeParameterWithTypeParameter)
                    TypeVariableBoundType(typeParameter.typeVariable)
                else StarProjectionBoundType

            BoundTypeTypeParameter(
                boundType,
                typeParameter.variance //TODO isMarkedNullable
            )
        }
}

internal class GenericBoundType(
    override val classReference: ClassReference,
    override val typeParameters: List<BoundTypeTypeParameter>,
    override val forcedNullabilityTo: Nullability? = null,
    val isNull: Boolean
) : BoundType

internal class LiteralBoundType(val isNull: Boolean) : BoundType {
    override val classReference = LiteralClassReference
    override val typeParameters: List<BoundTypeTypeParameter> = emptyList()
    override val forcedNullabilityTo: Nullability? = null
}


internal class TypeVariable(
    val typeElement: KtTypeElement?,
    val classReference: ClassReference,
    val typeParameters: List<TypeVariableTypeParameter>,
    var nullability: Nullability
)

internal inline val BoundType.bound
    get() =
        when {
            forcedNullabilityTo != null -> LiteralBound(forcedNullabilityTo!!)
            this is TypeVariableBoundType -> TypeVariableBound(typeVariable)
            this is LiteralBoundType -> LiteralBound(isNull.nullableForTrue())
            this is GenericBoundType -> LiteralBound(isNull.nullableForTrue())
            this is StarProjectionBoundType -> LiteralBound(Nullability.NULLABLE)
            else -> error("Bad bound type $this")
        }

internal inline fun <reified T : BoundType> T.withForcedNullability(nullability: Nullability?): T =
    if (forcedNullabilityTo == nullability || nullability == null) this
    else when (this) {
        is GenericBoundType ->
            GenericBoundType(
                classReference,
                typeParameters,
                nullability,
                isNull
            )
        is TypeVariableBoundType ->
            TypeVariableBoundType(
                typeVariable,
                nullability
            )
        is LiteralBoundType -> this
        else -> this
    } as T

internal interface TypeVariableTypeParameter {
    val variance: Variance
}

internal data class TypeVariableTypeParameterWithTypeParameter(
    val typeVariable: TypeVariable,
    override val variance: Variance
) : TypeVariableTypeParameter

internal object TypeVariableStartProjectionTypeParameter : TypeVariableTypeParameter {
    override val variance = Variance.INVARIANT //TODO ??
}


internal data class BoundTypeTypeParameter(
    val boundType: BoundType,
    val variance: Variance
)

internal object StarProjectionBoundType : BoundType {
    override val classReference = UnknownClassReference("*")//TODO
    override val typeParameters: List<BoundTypeTypeParameter> = emptyList()
    override val forcedNullabilityTo = null
}

internal interface Constraint {
    val cameFrom: ConstraintCameFrom
}

interface ConstraintBound

internal data class TypeVariableBound(val typeVariable: TypeVariable) : ConstraintBound
internal data class LiteralBound(val nullability: Nullability) : ConstraintBound

internal data class SubtypeConstraint(
    var lowerBound: ConstraintBound,
    var upperBound: ConstraintBound,
    override val cameFrom: ConstraintCameFrom
) : Constraint

enum class ConstraintCameFrom {
    SUPER_DECLARATION,
    INITIALIZER,
    COMPARED_WITH_NULL,
    ASSIGNMENT_TARGET,
    USED_AS_RECEIVER,
    PARAMETER_PASSED
}

internal data class EqualConstraint(
    val leftBound: ConstraintBound,
    val rightBound: ConstraintBound,
    override val cameFrom: ConstraintCameFrom
) : Constraint


private fun PsiTypeParameter.variance() = Variance.INVARIANT//TODO real variance

internal fun ClassReference.typeParametersVariance(parameterIndex: Int): Variance =
    when (this) {
        is KtClassReference -> klass.typeParameters.getOrNull(parameterIndex)?.variance ?: Variance.INVARIANT
        is JavaClassReference -> klass.typeParameters.getOrNull(parameterIndex)?.variance() ?: Variance.INVARIANT
        is DescriptorClassReference -> descriptor.typeConstructor.parameters[parameterIndex].variance
        is TypeParameterClassReference -> Variance.INVARIANT
        is UnknownClassReference -> Variance.INVARIANT
        else -> error("Bad class reference `$this`")
    }

internal fun TypeVariable.setNullabilityIfNotFixed(nullability: Nullability) {
    if (!isFixed) {
        this.nullability = nullability
    }
}

internal val TypeVariable.isFixed: Boolean
    get() = nullability != Nullability.UNKNOWN

enum class Nullability {
    NULLABLE, NOT_NULL, UNKNOWN
}

internal fun Boolean.nullableForTrue() =
    if (this) Nullability.NULLABLE else Nullability.NOT_NULL


internal fun TypeVariableOwner.innerTypeVariables(): List<TypeVariable> =
    when (this) {
        is SingleTypeVariableOwner -> typeVariable.innerTypeVariables()
        is MultipleTypeVariablesOwner -> typeVariables.flatMap { it.innerTypeVariables() }
        else -> error("")
    }

internal fun TypeVariable.innerTypeVariables(): List<TypeVariable> =
    typeParameters.flatMap { typeParameter ->
        typeParameter.safeAs<TypeVariableTypeParameterWithTypeParameter>()?.let { typeVariableTypeParameter ->
            typeVariableTypeParameter.typeVariable.innerTypeVariables() + listOfNotNull(typeVariableTypeParameter.typeVariable)
        }.orEmpty()
    }