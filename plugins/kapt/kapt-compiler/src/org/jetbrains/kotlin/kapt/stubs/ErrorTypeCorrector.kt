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

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.builtins.functions.BuiltInFunctionArity
import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmTypeMapper
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.toFirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.builder.buildUserTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirQualifierPartImpl
import org.jetbrains.kotlin.fir.types.impl.FirTypeArgumentListImpl
import org.jetbrains.kotlin.ir.types.IrErrorType
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.kapt.base.javac.kaptError
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.AbstractTypeMapper.hasNothingInNonContravariantPosition
import org.jetbrains.kotlin.types.AbstractTypeMapper.processGenericArguments
import org.jetbrains.kotlin.types.checker.SimpleClassicTypeSystemContext
import org.jetbrains.kotlin.types.error.ErrorTypeKind
import org.jetbrains.kotlin.types.error.ErrorUtils
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeParameterMarker
import org.jetbrains.kotlin.types.model.convertVariance
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.TypePath

private typealias SubstitutionScope = Map<String, TypeAliasArgumentBinding>

private data class TypeAliasArgumentBinding(
    val parameterVariance: Variance,
    val firProjection: FirTypeProjection,
    val coneProjection: ConeTypeProjection?,
    // the outer substitution scope in which this argument was written
    val substitutions: SubstitutionScope,
)

class ErrorTypeCorrector<Expression, Annotation : Expression>(
    private val converter: ParameterizedKaptStubConverter<*, Expression, *, *, *, Annotation, *, *, *, *>,
    firFile: FirFile,
) {
    private val defaultType = converter.makeQualifiedName(Any::class.java.name)
    private val fileSource = firFile.source

    private val aliasedImports = mutableMapOf<String, Expression>().apply {
        for (import in firFile.imports) {
            if (import.isAllUnder) continue

            val aliasName = import.aliasName?.asString() ?: continue
            val importedFqName = import.importedFqName ?: continue

            this[aliasName] = converter.makeQualifiedName(importedFqName)
        }
    }

    fun convert(
        typeRef: FirTypeRef,
        typeAnnotations: List<Pair<TypePath?, Annotation>>,
        typeMappingMode: TypeMappingMode,
        isVararg: Boolean = false,
    ): Expression = convert(typeRef, null, emptyMap(), TypeAnnotationCursor(typeAnnotations), typeMappingMode, isVararg)

    private fun convert(
        typeRef: FirTypeRef,
        coneType: ConeKotlinType?,
        substitutions: SubstitutionScope,
        typeAnnotations: TypeAnnotationCursor<Annotation>,
        typeMappingMode: TypeMappingMode,
        isVararg: Boolean = false,
    ): Expression {
        var innermostConeType: ConeKotlinType? = null
        var current: FirTypeRef? = typeRef
        while (current is FirResolvedTypeRef) {
            innermostConeType = current.coneType
            current = current.delegatedTypeRef
        }

        val resolvedType = coneType ?: innermostConeType
        val source = when (current) {
            is FirUserTypeRef -> current.source
            is FirFunctionTypeRef -> current.source
            else -> null
        }
        if (source != null && resolvedType != null && resolvedType !is ConeErrorType &&
            resolvedType.contains { it.abbreviatedType != null }
        ) {
            // Normalize written types before dispatch; annotation paths describe the expanded structure.
            val expandedRef = resolvedType.toExpandedTypeRef(converter.kaptContext.firSession!!, source)
            return convert(expandedRef, null, emptyMap(), typeAnnotations, typeMappingMode, isVararg)
        }
        return when (current) {
            is FirUserTypeRef -> convertUserType(current, resolvedType, substitutions, typeAnnotations, typeMappingMode, isVararg)
            is FirFunctionTypeRef -> convertFunctionType(current, resolvedType, substitutions, typeAnnotations, typeMappingMode)
            // inferred or other type without known written names
            is FirImplicitTypeRef, null ->
                if (resolvedType != null) convertConeType(resolvedType, typeAnnotations, typeMappingMode, isVararg) else defaultType
            // A replacement Object does not represent the original annotated type.
            else -> defaultType
        }
    }

    private fun convertConeType(
        coneType: ConeKotlinType,
        typeAnnotations: TypeAnnotationCursor<Annotation>,
        typeMappingMode: TypeMappingMode,
        isVararg: Boolean = false,
    ): Expression {
        val session = converter.kaptContext.firSession ?: return defaultType
        if (coneType is ConeClassLikeType && coneType !is ConeErrorType && coneType.contains {
                it.functionTypeKind(session) == FunctionTypeKind.SuspendFunction || it.isLargeFunctionType(session)
            }
        ) {
            // Reconstruct components so resolved types use the same recursive function/path lowering as written types.
            val expandedRef = coneType.toExpandedTypeRef(session, fileSource ?: return defaultType)
            return convert(expandedRef, null, emptyMap(), typeAnnotations, typeMappingMode, isVararg)
        }
        return converter.convertFirType(coneType, typeMappingMode, typeAnnotations.asRelativeTypeAnnotations(), isVararg) ?: defaultType
    }

    // Unresolved classes and absent class-type information must not consume annotation paths.
    private fun KotlinTypeMarker.hasDirectTypeArgumentMapping(argumentCount: Int): Boolean =
        this is ConeClassLikeType &&
                this !is ConeErrorType &&
                !isNonPrimitiveArray &&
                typeArguments.size == argumentCount

    private fun convertUserType(
        type: FirUserTypeRef,
        coneType: ConeKotlinType?,
        substitutions: SubstitutionScope,
        typeAnnotations: TypeAnnotationCursor<Annotation>,
        typeMappingMode: TypeMappingMode,
        isVararg: Boolean,
    ): Expression {
        val session = converter.kaptContext.firSession!!
        val functionKind = coneType?.functionTypeKind(session)
        if (coneType != null && (functionKind == FunctionTypeKind.Function || functionKind == FunctionTypeKind.SuspendFunction)) {
            val projections = type.qualifier.lastOrNull()?.typeArgumentList?.typeArguments.orEmpty()
            return convertFunctionType(type.source, coneType, projections, substitutions, typeAnnotations, typeMappingMode)
        }
        if (coneType != null && (coneType.isPrimitiveArray || coneType.isNonPrimitiveArray)) {
            return convertArrayType(type, coneType, substitutions, typeAnnotations, typeMappingMode, isVararg)
        }
        return convertQualifier(type.qualifier, type.qualifier.lastIndex, coneType, substitutions, typeAnnotations, typeMappingMode)
    }

    private fun convertArrayType(
        type: FirUserTypeRef,
        coneType: ConeKotlinType,
        substitutions: SubstitutionScope,
        typeAnnotations: TypeAnnotationCursor<Annotation>,
        typeMappingMode: TypeMappingMode,
        isVararg: Boolean,
    ): Expression {
        val session = converter.kaptContext.firSession!!
        val dimensions = mutableListOf<List<Annotation>>()
        var componentFirType: FirTypeRef = type
        var componentConeType = coneType
        var componentAnnotations = typeAnnotations
        var componentMode = typeMappingMode

        // Collect dimensions together: makeArrayType expects a non-array element and outermost-first annotations.
        while (componentConeType.isPrimitiveArray || componentConeType.isNonPrimitiveArray) {
            check(componentConeType.abbreviatedType == null) { "Array aliases must be expanded before array conversion" }
            when {
                componentConeType.isPrimitiveArray -> {
                    dimensions.add(componentAnnotations.annotationsHere())
                    componentConeType = requireNotNull(componentConeType.arrayElementType(checkUnsignedArrays = false))
                    componentFirType = componentConeType.toFirResolvedTypeRef()
                    componentAnnotations = componentAnnotations.arrayElement()
                    // Primitive array elements must stay unboxed, even in a generic context.
                    componentMode = TypeMappingMode.DEFAULT
                    break
                }

                componentConeType.isNonPrimitiveArray -> {
                    val writtenRef = componentFirType.unwrapResolvedTypeRef()
                    // convert() normalizes aliases, so written and resolved array components must agree.
                    val projection = checkNotNull(
                        (writtenRef as? FirUserTypeRef)?.qualifier?.lastOrNull()?.typeArgumentList?.typeArguments?.singleOrNull()
                    ) { "An expanded array type must have exactly one written type argument" }
                    val coneProjection = checkNotNull(componentConeType.typeArguments.singleOrNull()) {
                        "An expanded array type must have exactly one resolved type argument"
                    }
                    dimensions.add(componentAnnotations.annotationsHere())

                    // Match AbstractTypeMapper.mapArrayType: in-projected and star-projected arrays erase to Object[].
                    if (coneProjection.kind == ProjectionKind.IN || coneProjection.kind == ProjectionKind.STAR) {
                        // The backend can emit annotations on the erased component, but deeper paths no longer have a target.
                        val element = converter.makeClassType("java/lang/Object", componentAnnotations.arrayElement().annotationsHere())
                        return converter.makeArrayType(element, dimensions, isVararg)
                    }
                    val argumentFirType = checkNotNull((projection as? FirTypeProjectionWithVariance)?.typeRef) {
                        "A non-erased array component must have a written type reference"
                    }
                    val argumentConeType = (coneProjection as ConeKotlinTypeProjection).type
                    componentMode = componentMode.toGenericArgumentMode(
                        with(session.typeContext) { coneProjection.getVariance().convertVariance() }, ofArray = true,
                    )
                    componentFirType = argumentFirType
                    componentConeType = argumentConeType
                    componentAnnotations = componentAnnotations.arrayElement()
                }
            }
        }

        check(dimensions.isNotEmpty()) { "Array conversion requires at least one array dimension" }
        val element = convert(componentFirType, componentConeType, substitutions, componentAnnotations, componentMode)
        return converter.makeArrayType(element, dimensions, isVararg)
    }

    // [index] is the qualifier part being converted. It is -1 for an empty written qualifier,
    // where KAPT cannot recover the source type and falls back to [defaultType].
    private fun convertQualifier(
        parts: List<FirQualifierPart>, index: Int, coneType: ConeKotlinType?, substitutions: SubstitutionScope,
        typeAnnotations: TypeAnnotationCursor<Annotation>,
        typeMappingMode: TypeMappingMode,
    ): Expression {
        if (index < 0) return defaultType

        if (coneType != null) {
            return convertFirQualifier(parts, index, coneType, substitutions, typeAnnotations, typeMappingMode)
        }

        // Without a resolved type, discard unreliable JVM paths but preserve the supplied mapping mode.
        val part = parts[index]
        val referencedName = part.name.asString()

        if (index == 0) {
            if (referencedName in substitutions) {
                val binding = substitutions.getValue(referencedName)
                return convertTypeProjection(
                    binding.firProjection, null, binding.parameterVariance, binding.substitutions,
                    typeAnnotations = TypeAnnotationCursor(emptyList()), typeMappingMode = typeMappingMode,
                )
            }

            aliasedImports[referencedName]?.let { return it }
        }

        val baseExpression = if (index > 0) {
            val qualifierType = convertQualifier(
                parts, index - 1, null, substitutions,
                typeAnnotations = TypeAnnotationCursor(emptyList()), typeMappingMode = typeMappingMode,
            )
            if (qualifierType == defaultType) return defaultType
            converter.makeSelect(qualifierType, referencedName)
        } else converter.makeSimpleName(referencedName)

        val arguments = part.typeArgumentList.typeArguments
        if (arguments.isEmpty()) return baseExpression

        val convertedArguments = SimpleClassicTypeSystemContext.convertTypeArguments(
            arguments, null, ErrorUtils.createErrorType(ErrorTypeKind.KAPT_ERROR_TYPE), substitutions,
            typeAnnotations = TypeAnnotationCursor(emptyList()), typeMappingMode = typeMappingMode,
        )
        return converter.makeTypeApply(baseExpression, convertedArguments)
    }

    private fun TypeSystemCommonBackendContext.convertTypeArguments(
        arguments: List<FirTypeProjection>,
        typeParameters: List<TypeParameterMarker>?,
        type: KotlinTypeMarker,
        substitutions: SubstitutionScope,
        typeAnnotations: TypeAnnotationCursor<Annotation>,
        typeMappingMode: TypeMappingMode,
        argumentOffset: Int = 0,
    ): List<Expression> {
        // Positional pairing may be incorrect for non-matching arguments,
        // but remains our best effort when a direct mapping is unavailable.
        val typeArguments = type.getArguments().drop(argumentOffset)
        return buildList(arguments.size) {
            processGenericArguments(
                typeArguments.take(arguments.size), typeParameters.orEmpty().drop(argumentOffset), typeMappingMode,
                processUnboundedWildcard = {
                    // Each callback appends one argument, so size is the current argument's index.
                    add(annotateType(converter.makeUnboundWildcard(), typeAnnotations.typeArgument(size)))
                },
                processTypeArgument = { index, _, variance, _, childMode ->
                    add(
                        convertTypeProjection(
                            arguments[index], typeArguments[index] as? ConeTypeProjection, variance, substitutions,
                            typeAnnotations.typeArgument(index), childMode,
                        )
                    )
                },
            )
        }
    }

    private fun convertTypeProjection(
        projection: FirTypeProjection,
        coneProjection: ConeTypeProjection?,
        variance: Variance?,
        substitutions: SubstitutionScope,
        typeAnnotations: TypeAnnotationCursor<Annotation>,
        typeMappingMode: TypeMappingMode,
    ): Expression {
        fun unbounded(): Expression = annotateType(converter.makeUnboundWildcard(), typeAnnotations)

        val projectionWithVariance = projection as? FirTypeProjectionWithVariance ?: return unbounded()
        val argumentType = projectionWithVariance.typeRef
        val coneArgumentType = (coneProjection as? ConeKotlinTypeProjection)?.type
        val projectionVariance = projectionWithVariance.variance
        val wildcard = when {
            variance === Variance.INVARIANT -> null
            projectionVariance === Variance.IN_VARIANCE || variance === Variance.IN_VARIANCE -> '-'
            projectionVariance === Variance.OUT_VARIANCE || variance === Variance.OUT_VARIANCE -> '+'
            else -> null
        }
        return if (wildcard == null) {
            convert(argumentType, coneArgumentType, substitutions, typeAnnotations, typeMappingMode)
        } else {
            val argumentAnnotations = typeAnnotations.wildcardBound()
            val argumentExpression = convert(argumentType, coneArgumentType, substitutions, argumentAnnotations, typeMappingMode)
            annotateType(converter.makeWildcard(wildcard, argumentExpression), typeAnnotations)
        }
    }

    private fun annotateType(type: Expression, typeAnnotations: TypeAnnotationCursor<Annotation>): Expression {
        val annotations = typeAnnotations.annotationsHere()
        return if (annotations.isEmpty()) type else converter.makeAnnotatedType(type, annotations)
    }

    private fun convertFunctionType(
        type: FirFunctionTypeRef,
        coneType: ConeKotlinType?,
        substitutions: SubstitutionScope,
        typeAnnotations: TypeAnnotationCursor<Annotation>,
        typeMappingMode: TypeMappingMode,
    ): Expression {
        val components = buildList {
            addAll(type.contextParameterTypeRefs)
            type.receiverTypeRef?.let { add(it) }
            addAll(type.parameters.map { it.returnTypeRef })
            add(type.returnTypeRef)
        }

        if (coneType == null) {
            // With no JVM mapping information from coneType, guessing annotation paths is unreliable due to optional wildcards
            val arguments = components.map {
                convert(it, null, substitutions, TypeAnnotationCursor(emptyList()), typeMappingMode)
            }
            return converter.makeTypeApply(converter.makeSimpleName("Function${arguments.size - 1}"), arguments)
        }

        return convertFunctionType(
            type.source, coneType, components.map { it.toInvariantProjection() }, substitutions, typeAnnotations, typeMappingMode,
        )
    }

    private fun FirTypeRef.toInvariantProjection(): FirTypeProjectionWithVariance = buildTypeProjectionWithVariance {
        typeRef = this@toInvariantProjection
        variance = Variance.INVARIANT
    }

    private fun convertFunctionType(
        source: KtSourceElement,
        coneType: ConeKotlinType,
        projections: List<FirTypeProjection>,
        substitutions: SubstitutionScope,
        typeAnnotations: TypeAnnotationCursor<Annotation>,
        typeMappingMode: TypeMappingMode,
    ): Expression {
        val session = converter.kaptContext.firSession!!
        val functionKind = coneType.functionTypeKind(session)

        fun fallbackToJvmSignature(): Expression =
            convertFunctionTypeUsingJvmSignature(coneType, typeMappingMode, typeAnnotations.annotationsHereAsRoot())

        return when {
            // Written components cannot safely be paired with the resolved arguments (e.g. after alias expansion).
            !coneType.hasDirectTypeArgumentMapping(projections.size) -> fallbackToJvmSignature()

            // suspend functions require special handling due to special conversion of SuspendFunctionN -> Function(N+1)
            functionKind == FunctionTypeKind.SuspendFunction ->
                convertSuspendFunctionType(source, coneType, projections, substitutions, typeAnnotations, typeMappingMode)

            // Custom or unrecognized kinds need the complete mapped representation, including any legacy projection.
            // Keep only root annotations: nested JVM paths cannot be matched to written components without
            // accounting for the function kind's transformation. Such kinds need dedicated mapping and tests.
            functionKind != FunctionTypeKind.Function -> fallbackToJvmSignature()

            // Nothing and Nothing? in some positions may force raw FunctionN with no generic arguments
            hasNothingInNonContravariantPosition(session, coneType) -> fallbackToJvmSignature()

            // `FunctionM<..., R>` for `M>22` is converted to `FunctionN<R>`
            projections.size > BuiltInFunctionArity.BIG_ARITY -> convertLargeFunctionType(coneType, typeAnnotations, typeMappingMode)

            // Ordinary numbered function interfaces preserve all component positions.
            else -> convertMappedFunctionType(coneType, projections, substitutions, typeAnnotations, typeMappingMode)
        }
    }

    private fun ConeKotlinType.isLargeFunctionType(session: FirSession): Boolean =
        functionTypeKind(session) == FunctionTypeKind.Function && typeArguments.size > BuiltInFunctionArity.BIG_ARITY

    private fun convertLargeFunctionType(
        coneType: ConeKotlinType,
        typeAnnotations: TypeAnnotationCursor<Annotation>,
        mode: TypeMappingMode,
    ): Expression {
        val mappedAnnotations = buildList {
            addAll(typeAnnotations.annotationsHereAsRoot())
            if (!hasNothingInNonContravariantPosition(converter.kaptContext.firSession!!, coneType)) {
                val returnAnnotations = typeAnnotations.typeArgument(coneType.typeArguments.lastIndex).asRelativeTypeAnnotations()
                addAll(prependTypePath("0;", returnAnnotations))
            }
        }
        return convertFunctionTypeUsingJvmSignature(coneType, mode, mappedAnnotations)
    }

    private fun hasNothingInNonContravariantPosition(session: FirSession, coneType: ConeKotlinType): Boolean =
        with(session.typeContext) { hasNothingInNonContravariantPosition(coneType) }

    // Maps the complete resolved type, including legacy custom function-kind projection when required.
    // Annotation paths must already be aligned with the resulting JVM signature.
    private fun convertFunctionTypeUsingJvmSignature(
        coneType: ConeKotlinType,
        mode: TypeMappingMode,
        mappedAnnotations: List<Pair<TypePath?, Annotation>>,
    ): Expression = converter.projectLegacyFunctionTypeKindsIfNeeded(
        coneType.toFirResolvedTypeRef(), mode, mappedAnnotations,
    ) ?: converter.convertFirType(coneType, mode, mappedAnnotations) ?: defaultType

    private fun convertMappedFunctionType(
        coneType: ConeKotlinType,
        projections: List<FirTypeProjection>,
        substitutions: SubstitutionScope,
        typeAnnotations: TypeAnnotationCursor<Annotation>,
        mode: TypeMappingMode,
    ): Expression {
        val session = converter.kaptContext.firSession!!
        val asmType = FirJvmTypeMapper(session).mapType(coneType, mode)
        val functionType = converter.makeClassType(asmType.internalName, typeAnnotations.annotationsHere())
        val typeParameters = coneType.classId?.let(session.symbolProvider::getClassLikeSymbolByClassId)
            ?.typeParameterSymbols?.map { it.toLookupTag() }
        // convertTypeArguments gives each component its own cursor, including the receiver before ordinary parameters.
        val arguments = session.typeContext.convertTypeArguments(
            projections, typeParameters, coneType, substitutions, typeAnnotations, mode,
        )
        return converter.makeTypeApply(functionType, arguments)
    }

    private fun convertSuspendFunctionType(
        source: KtSourceElement,
        coneType: ConeKotlinType,
        projections: List<FirTypeProjection>,
        substitutions: SubstitutionScope,
        typeAnnotations: TypeAnnotationCursor<Annotation>,
        mode: TypeMappingMode,
    ): Expression {
        // The source suspend function has arguments SuspendFunctionN<P1, ..., P_N, R>.
        // Lower it to Function(N+1)<P1, ..., P_N, Continuation<R>, Any?> before applying JVM wildcard mapping.
        // P1, ..., P_N include context parameters and the extension receiver, followed by ordinary parameters.
        // For N + 1 > 22, the actual JVM interface is the arity-erased FunctionN<Any?> instead.
        val objectType = StandardClassIds.Any.constructClassLikeType(isMarkedNullable = true)
        if (projections.size >= BuiltInFunctionArity.BIG_ARITY) {
            // The continuation is an extra input, so 22 source inputs already require FunctionN<Object>.
            // No type annotations for original parameter types or return type R survive.
            val functionNType = StandardClassIds.Annotations.FunctionN.constructClassLikeType(arrayOf(objectType))
            return convertConeType(functionNType, TypeAnnotationCursor(typeAnnotations.annotationsHereAsRoot()), mode)
        }

        val typeMapper = FirJvmTypeMapper(converter.kaptContext.firSession!!)
        val sourceArguments = coneType.typeArguments.map { it.type ?: objectType }
        val runtimeType = typeMapper.buildSuspendFunctionRuntimeType(sourceArguments)

        val returnIndex = projections.lastIndex // R
        val mappedAnnotations = buildList {
            // Context parameters, the receiver, and ordinary inputs keep their indices, variances, and mapping modes.
            // Copy their paths (and the root annotations) unchanged; only R moves under Continuation.
            addAll(typeAnnotations.asRelativeTypeAnnotations().filterNot { [path, _] ->
                path != null && path.length > 0 && path.getStep(0) == TypePath.TYPE_ARGUMENT &&
                        path.getStepArgument(0) == returnIndex
            })
            addAll(remapReturnAnnotationsToContinuation(coneType, runtimeType, typeAnnotations, mode))
        }

        val objectProjection = objectType.toFirResolvedTypeRef().toInvariantProjection()

        // Reuse invariant projections; the JVM mapper discards use-site variance and replaces stars with `Any?`.
        fun FirTypeProjection.toRuntimeProjection(): FirTypeProjectionWithVariance = when {
            this !is FirTypeProjectionWithVariance -> objectProjection
            variance == Variance.INVARIANT -> this
            else -> typeRef.toInvariantProjection()
        }

        // Retain the written R reference for recursive error-type correction inside Continuation<R>.
        val continuationRef = buildUserTypeRef {
            this.source = source
            isMarkedNullable = false
            qualifier += FirQualifierPartImpl(null, StandardClassIds.Continuation.shortClassName, FirTypeArgumentListImpl(null).apply {
                typeArguments += projections.last().toRuntimeProjection()
            })
        }
        val runtimeProjections = buildList {
            for (index in 0 until returnIndex) add(projections[index].toRuntimeProjection())
            add(continuationRef.toInvariantProjection())
            add(objectProjection)
        }
        return convertMappedFunctionType(runtimeType, runtimeProjections, substitutions, TypeAnnotationCursor(mappedAnnotations), mode)
    }

    private class TypeArgumentMapping(val type: ConeKotlinType, val variance: Variance, val mode: TypeMappingMode)

    // Returns only R's annotations, relocated under Continuation<R> and relative to the lowered function type.
    private fun remapReturnAnnotationsToContinuation(
        sourceFunctionType: ConeKotlinType,
        runtimeFunctionType: ConeKotlinType,
        typeAnnotations: TypeAnnotationCursor<Annotation>,
        mode: TypeMappingMode,
    ): List<Pair<TypePath?, Annotation>> {
        if (typeAnnotations.isEmpty) return emptyList()
        val returnIndex = sourceFunctionType.typeArguments.lastIndex
        val sourceReturnMapping = sourceFunctionType.computeTypeArgumentMapping(returnIndex, mode)
            ?: return emptyList() // A star-projected return becomes Any? and has no source annotations.
        val continuationMapping: TypeArgumentMapping = requireNotNull(runtimeFunctionType.computeTypeArgumentMapping(returnIndex, mode))
        val returnMapping: TypeArgumentMapping = continuationMapping.type.computeTypeArgumentMapping(0, continuationMapping.mode)
            ?: return emptyList() // Continuation<Nothing> maps to Continuation<?> - no annotations survive
        val sourceCursor = typeAnnotations.atTypeArgument(returnIndex, sourceReturnMapping.variance)
        val prefix = argumentPath(returnIndex, continuationMapping.variance) + argumentPath(0, returnMapping.variance)
        // R changes from a function's covariant argument to Continuation's contravariant argument.
        // Recompute wildcard steps throughout R, not just at the newly introduced Continuation boundary.
        val annotations = remapTypeAnnotationPaths(
            sourceCursor, sourceReturnMapping.type, sourceReturnMapping.mode, returnMapping.mode,
        )
        return prependTypePath(prefix, annotations)
    }

    // An unbounded wildcard has no mapped argument type (e.g. Continuation<Nothing> becomes Continuation<?>).
    private fun ConeKotlinType.computeTypeArgumentMapping(index: Int, mode: TypeMappingMode): TypeArgumentMapping? {
        val session = converter.kaptContext.firSession!!
        val parameter = classId?.let(session.symbolProvider::getClassLikeSymbolByClassId)
            ?.typeParameterSymbols?.getOrNull(index)?.toLookupTag()
        var result: TypeArgumentMapping? = null
        with(session.typeContext) {
            processGenericArguments(
                listOf(typeArguments[index]), listOfNotNull(parameter), mode,
                processUnboundedWildcard = {},
                processTypeArgument = { _, type, variance, _, childMode ->
                    result = TypeArgumentMapping(type as ConeKotlinType, variance, childMode)
                },
            )
        }
        return result
    }

    private fun argumentPath(index: Int, variance: Variance): String =
        "$index;" + if (variance == Variance.INVARIANT) "" else "*"

    /** Prepends encoded path steps to the given annotations as a part of remapping them. */
    private fun prependTypePath(
        prefix: String,
        annotations: List<Pair<TypePath?, Annotation>>,
    ): List<Pair<TypePath?, Annotation>> = annotations.map { [path, annotation] ->
        TypePath.fromString(prefix + path?.toString().orEmpty()) to annotation
    }

    private fun TypeAnnotationCursor<Annotation>.atTypeArgument(index: Int, variance: Variance): TypeAnnotationCursor<Annotation> =
        typeArgument(index).let { if (variance == Variance.INVARIANT) it else it.wildcardBound() }

    // Remaps wildcard steps throughout the same Kotlin type subtree between mapping modes.
    // Returned paths are relative to the subtree's root.
    private fun remapTypeAnnotationPaths(
        annotations: TypeAnnotationCursor<Annotation>,
        subtreeType: ConeKotlinType,
        sourceMode: TypeMappingMode,
        targetMode: TypeMappingMode,
    ): List<Pair<TypePath?, Annotation>> {
        if (annotations.isEmpty) return emptyList()
        if (sourceMode == targetMode) return annotations.asRelativeTypeAnnotations()
        return buildList {
            addAll(annotations.annotationsHereAsRoot())
            for (index in subtreeType.typeArguments.indices) {
                val sourceMapping = subtreeType.computeTypeArgumentMapping(index, sourceMode) ?: continue
                val targetMapping = subtreeType.computeTypeArgumentMapping(index, targetMode) ?: continue
                val sourceCursor = if (subtreeType.isNonPrimitiveArray) annotations.arrayElement()
                else annotations.atTypeArgument(index, sourceMapping.variance)
                val prefix = if (subtreeType.isNonPrimitiveArray) "[" else argumentPath(index, targetMapping.variance)
                val childAnnotations = remapTypeAnnotationPaths(sourceCursor, sourceMapping.type, sourceMapping.mode, targetMapping.mode)
                addAll(prependTypePath(prefix, childAnnotations))
            }
        }
    }

    private fun FirTypeAlias.getSubstitutionScope(
        arguments: List<FirTypeProjection>, coneType: ConeKotlinType?, substitutions: SubstitutionScope,
    ): SubstitutionScope {
        if (typeParameters.size != arguments.size) {
            val kaptContext = converter.kaptContext
            val error = kaptContext.kaptError("${typeParameters.size} parameters are expected but ${arguments.size} passed")
            kaptContext.compiler.log.report(error)
            return emptyMap()
        }

        val newSubstitutionScope = mutableMapOf<String, TypeAliasArgumentBinding>()

        for ([index, typeParameterRef] in typeParameters.withIndex()) {
            val typeParameter = typeParameterRef as? FirTypeParameter ?: continue
            newSubstitutionScope[typeParameter.name.asString()] =
                TypeAliasArgumentBinding(typeParameter.variance, arguments[index], coneType?.typeArguments?.getOrNull(index), substitutions)
        }

        return newSubstitutionScope
    }

    @OptIn(SymbolInternals::class)
    private fun convertFirQualifier(
        parts: List<FirQualifierPart>, index: Int, coneType: ConeKotlinType, substitutions: SubstitutionScope,
        typeAnnotations: TypeAnnotationCursor<Annotation>,
        typeMappingMode: TypeMappingMode,
    ): Expression {
        require(index >= 0) { "Fir qualifier calculation requires positive index, but got $index" }

        val session = converter.kaptContext.firSession!!

        val part = parts[index]
        val abbreviatedType = coneType.abbreviatedType

        val baseExpression = when {
            abbreviatedType != null && coneType is ConeErrorType -> {
                val firTypeAlias = abbreviatedType.classId?.let(session.symbolProvider::getClassLikeSymbolByClassId) as? FirTypeAliasSymbol
                val typeAlias = firTypeAlias?.fir ?: return defaultType
                val newSubstitutionScope =
                    typeAlias.getSubstitutionScope(part.typeArgumentList.typeArguments, abbreviatedType, substitutions)
                return convert(
                    typeAlias.expandedTypeRef, null, newSubstitutionScope,
                    TypeAnnotationCursor(emptyList()), typeMappingMode,
                )
            }
            coneType is ConeClassLikeType && coneType !is ConeErrorType ->
                return convertResolvedClassQualifier(parts, index, coneType, substitutions, typeAnnotations, typeMappingMode, session)
            else -> {
                val referencedName = part.name.asString()

                if (index == 0) {
                    if (referencedName in substitutions) {
                        val binding = substitutions.getValue(referencedName)
                        return convertTypeProjection(
                            binding.firProjection, binding.coneProjection, binding.parameterVariance, binding.substitutions,
                            TypeAnnotationCursor(emptyList()), typeMappingMode,
                        )
                    }

                    aliasedImports[referencedName]?.let { return it }
                }

                if (index > 0) {
                    // The unresolved qualifier cannot reuse the leaf type's annotation paths.
                    val qualifierType = convertQualifier(
                        parts, index - 1, coneType, substitutions,
                        TypeAnnotationCursor(emptyList()),
                        typeMappingMode,
                    )
                    if (qualifierType == defaultType) return defaultType
                    converter.makeSelect(qualifierType, referencedName)
                } else converter.makeSimpleName(referencedName)
            }
        }

        val arguments = part.typeArgumentList.typeArguments
        if (arguments.isEmpty()) {
            return if (coneType is ConeTypeParameterType) annotateType(baseExpression, typeAnnotations) else baseExpression
        }

        val typeParameters =
            coneType.classId?.let(session.symbolProvider::getClassLikeSymbolByClassId)?.typeParameterSymbols?.map { it.toLookupTag() }
        val convertedArguments = session.typeContext.convertTypeArguments(
            arguments, typeParameters, coneType, substitutions, TypeAnnotationCursor(emptyList()), typeMappingMode,
        )
        return converter.makeTypeApply(baseExpression, convertedArguments)
    }

    private fun convertResolvedClassQualifier(
        parts: List<FirQualifierPart>, index: Int, coneType: ConeClassLikeType, substitutions: SubstitutionScope,
        typeAnnotations: TypeAnnotationCursor<Annotation>, typeMappingMode: TypeMappingMode, session: FirSession,
    ): Expression {
        // Arrays are handled by convertArrayType; generic-argument mapping boxes primitives and value classes,
        // so only class types are expected here.
        val asmType = FirJvmTypeMapper(session).mapType(coneType, TypeMappingMode.GENERIC_ARGUMENT)
        check(asmType.sort == Type.OBJECT) { "Expected a class type for $coneType, but mapped to $asmType" }
        val resolvedName = converter.treeMaker.resolveQualifiedName(asmType.internalName)
        val classInfo = resolvedName.classInfo

        // In Kotlin, a qualifier for a chain of nested classes may be split to "flat prefix" (outer +
        // optional static nested classes) and inner tail, like `Outer.StaticNested<T1>.Inner1<T2>.Inner2<T3>`
        // -- here flatPrefix is `Outer.StaticNested<T1>`.
        // Keep the classes in flat prefix together as a single step in annotation paths, then advance one INNER_TYPE step for each
        // trailing inner class.
        val namesInFlatPrefix = classInfo.indexOfLast { !it.isInner } + 1
        // In a valid Kotlin qualifier, classes before the last static class have no written type arguments.
        val expectedArgumentCount = parts.subList(0, index + 1).sumOf { it.typeArgumentList.typeArguments.size }
        val hasDirectArgumentMapping = coneType.hasDirectTypeArgumentMapping(expectedArgumentCount)
        val typeParameters = coneType.classId.let(session.symbolProvider::getClassLikeSymbolByClassId)
            ?.typeParameterSymbols?.map { it.toLookupTag() }
        // FIR qualifiers and classInfo may potentially have different number of elements, e.g. if `Outer.Nested` part
        // is imported, so we need this translation offset
        val classInfoToQualifierPartsTranslationOffset = index - classInfo.lastIndex
        // index of flat prefix's last qualifier in `parts`. It may be negative only if the flat prefix has no generic arguments
        val flatPartIndex = classInfoToQualifierPartsTranslationOffset + namesInFlatPrefix - 1

        fun applyArguments(expression: Expression, partIndex: Int, cursor: TypeAnnotationCursor<Annotation>): Expression {
            // If counts differ, only the leaf's arguments can be paired; class-component annotations still survive.
            if (!hasDirectArgumentMapping && partIndex != index) return expression
            val arguments = parts.getOrNull(partIndex)?.typeArgumentList?.typeArguments.orEmpty()
            if (arguments.isEmpty()) return expression
            // FIR stores the innermost class's arguments first, followed by enclosing-class arguments.
            val argumentOffset = parts.subList(partIndex + 1, index + 1)
                .sumOf { it.typeArgumentList.typeArguments.size }
            val converted = session.typeContext.convertTypeArguments(
                arguments, typeParameters, coneType, substitutions,
                if (hasDirectArgumentMapping) cursor else TypeAnnotationCursor(emptyList()),
                typeMappingMode, argumentOffset,
            )
            return converter.makeTypeApply(expression, converted)
        }

        val flatInternalName = if (namesInFlatPrefix == classInfo.size) asmType.internalName else buildString {
            if (resolvedName.packageName.isNotEmpty()) {
                append(resolvedName.packageName.replace('.', '/'))
                append('/')
            }
            append(classInfo.take(namesInFlatPrefix).joinToString("$") { it.name })
        }
        var cursor = typeAnnotations
        var expression = applyArguments(converter.makeClassType(flatInternalName, cursor.annotationsHere()), flatPartIndex, cursor)
        for (innerIndex in namesInFlatPrefix until classInfo.size) {
            cursor = cursor.innerType()
            expression = applyArguments(
                converter.makeInnerClassType(expression, classInfo[innerIndex].name, cursor.annotationsHere()),
                classInfoToQualifierPartsTranslationOffset + innerIndex, cursor,
            )
        }
        return expression
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
