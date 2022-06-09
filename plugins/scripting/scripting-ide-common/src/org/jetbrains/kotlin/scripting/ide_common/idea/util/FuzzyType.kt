/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("FuzzyTypeUtils")

package org.jetbrains.kotlin.scripting.ide_common.idea.util

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.resolve.calls.inference.components.*
import org.jetbrains.kotlin.resolve.calls.inference.model.KnownTypeParameterConstraintPositionImpl
import org.jetbrains.kotlin.resolve.calls.inference.model.TypeVariableFromCallableDescriptor
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner
import org.jetbrains.kotlin.types.checker.SimpleClassicTypeSystemContext
import org.jetbrains.kotlin.types.checker.SimpleClassicTypeSystemContext.safeSubstitute
import org.jetbrains.kotlin.types.error.ErrorUtils
import org.jetbrains.kotlin.types.typeUtil.*
import java.util.*

fun CallableDescriptor.fuzzyExtensionReceiverType(languageVersionSettings: LanguageVersionSettings): FuzzyType? =
    extensionReceiverParameter?.type?.toFuzzyType(typeParameters, languageVersionSettings)

fun FuzzyType.nullability() = type.nullability()

fun KotlinType.toFuzzyType(
    freeParameters: Collection<TypeParameterDescriptor>,
    languageVersionSettings: LanguageVersionSettings
): FuzzyType = FuzzyType(this, freeParameters, languageVersionSettings)

class FuzzyType(
    val type: KotlinType,
    freeParameters: Collection<TypeParameterDescriptor>,
    private val languageVersionSettings: LanguageVersionSettings
) {
    val freeParameters: Set<TypeParameterDescriptor>

    private val constraintInjector: ConstraintInjector

    init {
        val builtIns = type.builtIns
        val typeApproximator = TypeApproximator(builtIns, languageVersionSettings)
        val constraintIncorporator = ConstraintIncorporator(
            typeApproximator,
            TrivialConstraintTypeInferenceOracle.create(SimpleClassicTypeSystemContext),
            ClassicConstraintSystemUtilContext(KotlinTypeRefiner.Default, builtIns)
        )
        constraintInjector = ConstraintInjector(constraintIncorporator, typeApproximator, languageVersionSettings)

        if (freeParameters.isNotEmpty()) {
            // we allow to pass type parameters from another function with the same original in freeParameters
            val usedTypeParameters = HashSet<TypeParameterDescriptor>().apply { addUsedTypeParameters(type) }
            if (usedTypeParameters.isNotEmpty()) {
                val originalFreeParameters = freeParameters.map { it.toOriginal() }.toSet()
                this.freeParameters = usedTypeParameters.filter { it.toOriginal() in originalFreeParameters }.toSet()
            } else {
                this.freeParameters = emptySet()
            }
        } else {
            this.freeParameters = emptySet()
        }
    }

    // Diagnostic for EA-109046
    @Suppress("USELESS_ELVIS")
    private fun TypeParameterDescriptor.toOriginal(): TypeParameterDescriptor {
        val callableDescriptor = containingDeclaration as? CallableMemberDescriptor ?: return this
        val original = callableDescriptor.original ?: error("original = null for $callableDescriptor")
        val typeParameters = original.typeParameters ?: error("typeParameters = null for $original")
        return typeParameters[index]
    }

    override fun equals(other: Any?) = other is FuzzyType && other.type == type && other.freeParameters == freeParameters

    override fun hashCode() = type.hashCode()

    private fun MutableSet<TypeParameterDescriptor>.addUsedTypeParameters(type: KotlinType) {
        val typeParameter = type.constructor.declarationDescriptor as? TypeParameterDescriptor
        if (typeParameter != null && add(typeParameter)) {
            typeParameter.upperBounds.forEach { addUsedTypeParameters(it) }
        }

        for (argument in type.arguments) {
            if (!argument.isStarProjection) { // otherwise we can fall into infinite recursion
                addUsedTypeParameters(argument.type)
            }
        }
    }

    @Suppress("MemberVisibilityCanBePrivate") // Used in intellij-community
    fun checkIsSubtypeOf(otherType: FuzzyType): TypeSubstitutor? = matchedSubstitutor(otherType, MatchKind.IS_SUBTYPE)

    @Suppress("MemberVisibilityCanBePrivate") // Used in intellij-community
    fun checkIsSuperTypeOf(otherType: FuzzyType): TypeSubstitutor? = matchedSubstitutor(otherType, MatchKind.IS_SUPERTYPE)

    @Suppress("unused") // Used in intellij-community
    fun checkIsSubtypeOf(otherType: KotlinType): TypeSubstitutor? =
        checkIsSubtypeOf(otherType.toFuzzyType(emptyList(), languageVersionSettings))

    fun checkIsSuperTypeOf(otherType: KotlinType): TypeSubstitutor? =
        checkIsSuperTypeOf(otherType.toFuzzyType(emptyList(), languageVersionSettings))

    private enum class MatchKind {
        IS_SUBTYPE,
        IS_SUPERTYPE
    }

    private fun matchedSubstitutor(otherType: FuzzyType, matchKind: MatchKind): TypeSubstitutor? {
        if (type.isError) return null
        if (otherType.type.isError) return null
        if (otherType.type.isUnit() && matchKind == MatchKind.IS_SUBTYPE) return TypeSubstitutor.EMPTY

        fun KotlinType.checkInheritance(otherType: KotlinType): Boolean {
            return when (matchKind) {
                MatchKind.IS_SUBTYPE -> this.isSubtypeOf(otherType)
                MatchKind.IS_SUPERTYPE -> otherType.isSubtypeOf(this)
            }
        }

        if (freeParameters.isEmpty() && otherType.freeParameters.isEmpty()) {
            return if (type.checkInheritance(otherType.type)) TypeSubstitutor.EMPTY else null
        }

        val constraintSystem = SimpleConstraintSystemImpl(
            constraintInjector, type.builtIns, KotlinTypeRefiner.Default, languageVersionSettings
        )
        val builder = constraintSystem.csBuilder
        val typeVariableSubstitutor = constraintSystem.registerTypeVariables(freeParameters + otherType.freeParameters)

        val typeInSystem = typeVariableSubstitutor.safeSubstitute(type)
        val otherTypeInSystem = typeVariableSubstitutor.safeSubstitute(otherType.type)

        when (matchKind) {
            MatchKind.IS_SUBTYPE ->
                builder.addSubtypeConstraint(typeInSystem, otherTypeInSystem, KnownTypeParameterConstraintPositionImpl(type))
            MatchKind.IS_SUPERTYPE ->
                builder.addSubtypeConstraint(otherTypeInSystem, typeInSystem, KnownTypeParameterConstraintPositionImpl(type))
        }

        with(constraintSystem) {
            system.asConstraintSystemCompleterContext()
            fixAllTypeVariables()
        }

        if (constraintSystem.hasContradiction()) return null

        // currently ConstraintSystem return successful status in case there are problems with nullability
        // that's why we have to check subtyping manually
        val substitutor = NewTypeSubstitutorByConstructorMap(
            (constraintSystem.system.buildCurrentSubstitutor() as NewTypeSubstitutorByConstructorMap).map.mapKeys { (typeVariable, _) ->
                (typeVariable as TypeVariableTypeConstructor).originalTypeParameter!!.typeConstructor
            }
        )

        val substitutedType = substitutor.substituteArgumentProjection(type.asTypeProjection())?.type ?: return null
        if (substitutedType.isError) return TypeSubstitutor.EMPTY
        val otherSubstitutedType = substitutor.substituteArgumentProjection(otherType.type.asTypeProjection())?.type ?: return null
        if (otherSubstitutedType.isError) return TypeSubstitutor.EMPTY
        if (!substitutedType.checkInheritance(otherSubstitutedType)) return null

        val substitution = TypeConstructorSubstitution.createByConstructorsMap(substitutor.map.mapValues { it.value.asTypeProjection() })
        val substitutorToKeepCapturedTypes = object : DelegatedTypeSubstitution(substitution) {
            override fun approximateCapturedTypes() = false
        }.buildSubstitutor()

        val substitutionMap: Map<TypeConstructor, TypeProjection> = constraintSystem.system.allTypeVariables.values
            .map { (it as TypeVariableFromCallableDescriptor).originalTypeParameter }
            .associateBy(
                keySelector = { it.typeConstructor },
                valueTransform = {
                    val typeProjection = TypeProjectionImpl(Variance.INVARIANT, it.defaultType)
                    val substitutedProjection = substitutorToKeepCapturedTypes.substitute(typeProjection)
                    substitutedProjection?.takeUnless { ErrorUtils.containsUninferredTypeVariable(it.type) } ?: typeProjection
                })
        return TypeConstructorSubstitution.createByConstructorsMap(substitutionMap, approximateCapturedTypes = true).buildSubstitutor()
    }
}
