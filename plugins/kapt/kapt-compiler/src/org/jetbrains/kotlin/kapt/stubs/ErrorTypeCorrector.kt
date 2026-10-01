/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.kapt.stubs

import org.jetbrains.kotlin.fir.backend.jvm.FirJvmTypeMapper
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.ir.types.IrErrorType
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.kapt.base.javac.kaptError
import org.jetbrains.kotlin.kapt.stubs.ErrorTypeCorrector.TypeKind.*
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.load.kotlin.getOptimalModeForReturnType
import org.jetbrains.kotlin.load.kotlin.getOptimalModeForValueParameter
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.AbstractTypeMapper.getVarianceForWildcard
import org.jetbrains.kotlin.types.checker.SimpleClassicTypeSystemContext
import org.jetbrains.kotlin.types.error.ErrorTypeKind
import org.jetbrains.kotlin.types.error.ErrorUtils
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeParameterMarker

private typealias SubstitutionMap = Map<String, Triple<Variance, FirTypeProjection, ConeTypeProjection?>>

class ErrorTypeCorrector<Expression>(
    private val converter: ParameterizedKaptStubConverter<*, Expression, *, *, *, *, *, *, *, *>,
    private val typeKind: TypeKind,
    firFile: FirFile,
) {
    private val defaultType = converter.makeQualifiedName(Any::class.java.name)

    private val aliasedImports = mutableMapOf<String, Expression>().apply {
        for (import in firFile.imports) {
            if (import.isAllUnder) continue

            val aliasName = import.aliasName?.asString() ?: continue
            val importedFqName = import.importedFqName ?: continue

            this[aliasName] = converter.makeQualifiedName(importedFqName)
        }
    }

    enum class TypeKind {
        RETURN_TYPE, METHOD_PARAMETER_TYPE, SUPER_TYPE, ANNOTATION
    }

    fun convert(typeRef: FirTypeRef): Expression = convert(typeRef, null, emptyMap())

    private fun convert(typeRef: FirTypeRef, coneType: ConeKotlinType?, substitutions: SubstitutionMap): Expression {
        var innermostConeType: ConeKotlinType? = null
        var current: FirTypeRef? = typeRef
        while (current is FirResolvedTypeRef) {
            innermostConeType = current.coneType
            current = current.delegatedTypeRef
        }

        val resolvedType = coneType ?: innermostConeType
        return when (current) {
            is FirUserTypeRef -> convertUserType(current, resolvedType, substitutions)
            is FirFunctionTypeRef -> convertFunctionType(current, resolvedType, substitutions)
            null, is FirImplicitTypeRef -> convertInferredType(resolvedType)
            else -> defaultType
        }
    }

    private fun convertInferredType(coneType: ConeKotlinType?): Expression {
        if (coneType == null) return defaultType
        val typeContext = converter.kaptContext.firSession?.typeContext ?: return defaultType
        val typeMappingMode = with(typeContext) {
            when (typeKind) {
                RETURN_TYPE -> getOptimalModeForReturnType(coneType, false)
                METHOD_PARAMETER_TYPE -> getOptimalModeForValueParameter(coneType)
                SUPER_TYPE -> TypeMappingMode.SUPER_TYPE
                ANNOTATION -> TypeMappingMode.DEFAULT
            }
        }
        return converter.convertFirType(coneType, typeMappingMode) ?: defaultType
    }

    private fun convertUserType(type: FirUserTypeRef, coneType: ConeKotlinType?, substitutions: SubstitutionMap): Expression =
        convertQualifier(type.qualifier, type.qualifier.lastIndex, coneType, substitutions)

    // [index] is the qualifier part being converted. It is -1 for an empty written qualifier,
    // where KAPT cannot recover the source type and falls back to [defaultType].
    private fun convertQualifier(
        parts: List<FirQualifierPart>, index: Int, coneType: ConeKotlinType?, substitutions: SubstitutionMap,
    ): Expression {
        if (index < 0) return defaultType

        if (coneType != null) {
            return convertFirQualifier(parts, index, coneType, substitutions)
        }

        val part = parts[index]
        val referencedName = part.name.asString()

        if (index == 0) {
            if (referencedName in substitutions) {
                val [variance, projection] = substitutions.getValue(referencedName)
                return convertTypeProjection(projection, null, variance, emptyMap())
            }

            aliasedImports[referencedName]?.let { return it }
        }

        val baseExpression = if (index > 0) {
            val qualifierType = convertQualifier(parts, index - 1, null, substitutions)
            if (qualifierType == defaultType) return defaultType
            converter.makeSelect(qualifierType, referencedName)
        } else converter.makeSimpleName(referencedName)

        val arguments = part.typeArgumentList.typeArguments
        if (arguments.isEmpty()) return baseExpression

        val convertedArguments = SimpleClassicTypeSystemContext.convertTypeArguments(
            arguments, null, ErrorUtils.createErrorType(ErrorTypeKind.KAPT_ERROR_TYPE), substitutions
        )
        return converter.makeTypeApply(baseExpression, convertedArguments)
    }

    private fun TypeSystemCommonBackendContext.convertTypeArguments(
        arguments: List<FirTypeProjection>,
        typeParameters: List<TypeParameterMarker>?,
        type: KotlinTypeMarker,
        substitutions: SubstitutionMap,
    ): List<Expression> = arguments.mapIndexed { index, projection ->
        val typeMappingMode = when (typeKind) {
            //TODO figure out if the containing method is an annotation method
            RETURN_TYPE -> getOptimalModeForReturnType(type, false)
            METHOD_PARAMETER_TYPE -> getOptimalModeForValueParameter(type)
            SUPER_TYPE -> TypeMappingMode.SUPER_TYPE
            ANNOTATION -> TypeMappingMode.DEFAULT // see genAnnotation in org/jetbrains/kotlin/codegen/AnnotationCodegen.java
        }.updateArgumentModeFromAnnotations(type, this)

        val typeParameter = typeParameters?.getOrNull(index)
        val typeArgument = type.getArguments().getOrNull(index)
        val variance = if (typeArgument != null && typeParameter != null && !typeArgument.isStarProjection()) {
            getVarianceForWildcard(typeParameter, typeArgument, typeMappingMode)
        } else {
            null
        }
        convertTypeProjection(projection, typeArgument as? ConeTypeProjection, variance, substitutions)
    }

    private fun convertTypeProjection(
        projection: FirTypeProjection,
        coneProjection: ConeTypeProjection?,
        variance: Variance?,
        substitutions: SubstitutionMap,
    ): Expression {
        fun unbounded(): Expression = converter.makeUnboundWildcard()

        val projectionWithVariance = projection as? FirTypeProjectionWithVariance ?: return unbounded()
        val argumentType = projectionWithVariance.typeRef
        val coneArgumentType = (coneProjection as? ConeKotlinTypeProjection)?.type
        val argumentExpression by lazy { convert(argumentType, coneArgumentType, substitutions) }

        if (variance === Variance.INVARIANT) {
            return argumentExpression
        }

        val projectionVariance = projectionWithVariance.variance

        return when {
            projectionVariance === Variance.IN_VARIANCE || variance === Variance.IN_VARIANCE ->
                converter.makeWildcard('-', argumentExpression)

            projectionVariance === Variance.OUT_VARIANCE || variance === Variance.OUT_VARIANCE ->
                converter.makeWildcard('+', argumentExpression)

            else -> argumentExpression // invariant
        }
    }

    private fun convertFunctionType(
        type: FirFunctionTypeRef, coneType: ConeKotlinType?, substitutions: SubstitutionMap,
    ): Expression {
        val receiverType = type.receiverTypeRef
        val coneTypeArguments = (coneType as? ConeClassLikeType)?.typeArguments
        val parameterTypes = type.parameters.withIndex().map { [index, parameter] ->
            convert(
                parameter.returnTypeRef,
                (coneTypeArguments?.getOrNull(index + if (receiverType != null) 1 else 0) as? ConeKotlinTypeProjection)?.type,
                substitutions
            )
        }
        val returnType = convert(
            type.returnTypeRef,
            (coneTypeArguments?.lastOrNull() as? ConeKotlinTypeProjection)?.type,
            substitutions,
        )

        val allTypeArguments = buildList {
            if (receiverType != null) {
                add(
                    convert(receiverType, (coneTypeArguments?.firstOrNull() as? ConeKotlinTypeProjection)?.type, substitutions)
                )
            }
            addAll(parameterTypes)
            add(returnType)
        }

        val name = "Function" + (allTypeArguments.size - 1)
        return converter.makeTypeApply(converter.makeSimpleName(name), allTypeArguments)
    }

    private fun FirTypeAlias.getSubstitutions(arguments: List<FirTypeProjection>, coneType: ConeKotlinType?): SubstitutionMap {
        if (typeParameters.size != arguments.size) {
            val kaptContext = converter.kaptContext
            val error = kaptContext.kaptError("${typeParameters.size} parameters are expected but ${arguments.size} passed")
            kaptContext.compiler.log.report(error)
            return emptyMap()
        }

        val substitutionMap = mutableMapOf<String, Triple<Variance, FirTypeProjection, ConeTypeProjection?>>()

        for ([index, typeParameterRef] in typeParameters.withIndex()) {
            val typeParameter = typeParameterRef as? FirTypeParameter ?: continue
            substitutionMap[typeParameter.name.asString()] =
                Triple(typeParameter.variance, arguments[index], coneType?.typeArguments?.getOrNull(index))
        }

        return substitutionMap
    }

    @OptIn(SymbolInternals::class)
    private fun convertFirQualifier(
        parts: List<FirQualifierPart>, index: Int, coneType: ConeKotlinType, substitutions: SubstitutionMap,
    ): Expression {
        require(index >= 0) { "Fir qualifier calculation requires positive index, but got $index" }

        val session = converter.kaptContext.firSession!!

        val part = parts[index]
        val abbreviatedType = coneType.abbreviatedType

        val baseExpression = when {
            abbreviatedType != null && coneType is ConeErrorType -> {
                val firTypeAlias = abbreviatedType.classId?.let(session.symbolProvider::getClassLikeSymbolByClassId) as? FirTypeAliasSymbol
                val typeAlias = firTypeAlias?.fir ?: return defaultType
                val newSubstitutions = typeAlias.getSubstitutions(part.typeArgumentList.typeArguments, abbreviatedType)
                return convert(typeAlias.expandedTypeRef, null, newSubstitutions)
            }
            coneType is ConeClassLikeType && coneType !is ConeErrorType -> {
                // Nested under an error type, so keep primitives boxed.
                val asmType = FirJvmTypeMapper(session).mapType(coneType, TypeMappingMode.GENERIC_ARGUMENT)
                converter.makeType(asmType)
            }
            else -> {
                val referencedName = part.name.asString()

                if (index == 0) {
                    if (referencedName in substitutions) {
                        val [variance, projection, coneProjection] = substitutions.getValue(referencedName)
                        return convertTypeProjection(projection, coneProjection, variance, emptyMap())
                    }

                    aliasedImports[referencedName]?.let { return it }
                }

                if (index > 0) {
                    val qualifierType = convertQualifier(parts, index - 1, coneType, substitutions)
                    if (qualifierType == defaultType) return defaultType
                    converter.makeSelect(qualifierType, referencedName)
                } else converter.makeSimpleName(referencedName)
            }
        }

        val arguments = part.typeArgumentList.typeArguments
        if (arguments.isEmpty()) return baseExpression

        val typeParameters =
            coneType.classId?.let(session.symbolProvider::getClassLikeSymbolByClassId)?.typeParameterSymbols?.map { it.toLookupTag() }
        val convertedArguments = session.typeContext.convertTypeArguments(arguments, typeParameters, coneType, substitutions)
        return converter.makeTypeApply(baseExpression, convertedArguments)
    }
}

fun KotlinType.containsErrorTypes(allowedDepth: Int = 10): Boolean {
    // Need to limit recursion depth in case of complex recursive generics
    if (allowedDepth <= 0) {
        return false
    }

    if (this.isError) return true
    if (this.arguments.any { !it.isStarProjection && it.type.containsErrorTypes(allowedDepth - 1) }) return true
    return false
}

@Suppress("RedundantIf")
fun IrType.containsErrorTypes(allowedDepth: Int = 10): Boolean {
    if (allowedDepth <= 0) return false
    return this is IrErrorType ||
            this is IrSimpleType && arguments.any { it.typeOrNull?.containsErrorTypes(allowedDepth - 1) == true }
}
