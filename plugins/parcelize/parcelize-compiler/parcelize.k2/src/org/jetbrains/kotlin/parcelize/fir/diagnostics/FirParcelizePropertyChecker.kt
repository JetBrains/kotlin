/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize.fir.diagnostics

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.isEnumClass
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.primaryConstructorIfAny
import org.jetbrains.kotlin.fir.declarations.utils.fromPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.utils.hasBackingField
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.declarations.utils.isData
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.java.hasJvmFieldAnnotation
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.parcelize.BuiltinParcelableTypes
import org.jetbrains.kotlin.parcelize.ParcelizeNames
import org.jetbrains.kotlin.parcelize.ParcelizeNames.CREATOR_NAME
import org.jetbrains.kotlin.parcelize.ParcelizeNames.IGNORED_ON_PARCEL_FQ_NAMES
import org.jetbrains.kotlin.parcelize.ParcelizeNames.PARCELER_ID
import org.jetbrains.kotlin.parcelize.fir.dataClassPrimaryConstructorPropertiesOrNull

object FirParcelizePropertyChecker : FirPropertyChecker(MppCheckerKind.Platform) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirProperty) {
        val session = context.session
        val containingClassSymbol = declaration.dispatchReceiverType?.toRegularClassSymbol() ?: return

        if (containingClassSymbol.isParcelize(session)) {
            val fromPrimaryConstructor = declaration.fromPrimaryConstructor ?: false
            if (
                !fromPrimaryConstructor &&
                (declaration.hasBackingField || declaration.delegate != null) &&
                !declaration.hasIgnoredOnParcel(session) &&
                !containingClassSymbol.hasCustomParceler(session)
            ) {
                reporter.reportOn(declaration.source, KtErrorsParcelize.PROPERTY_WONT_BE_SERIALIZED)
            }
            if (fromPrimaryConstructor) {
                checkParcelableClassProperty(declaration, containingClassSymbol, context, reporter)
            }
        }

        if (declaration.name == CREATOR_NAME && containingClassSymbol.isCompanion && declaration.hasJvmFieldAnnotation(session)) {
            val outerClass = context.containingDeclarations.asReversed().getOrNull(1) as? FirRegularClassSymbol
            if (outerClass != null && outerClass.isParcelize(session)) {
                reporter.reportOn(declaration.source, KtErrorsParcelize.CREATOR_DEFINITION_IS_NOT_ALLOWED)
            }
        }
    }

    private fun checkParcelableClassProperty(
        property: FirProperty,
        containingClassSymbol: FirRegularClassSymbol,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val session = context.session
        val type = property.returnTypeRef.coneType.fullyExpandedType(session)
        if (type is ConeErrorType || containingClassSymbol.hasCustomParceler(session) || property.hasIgnoredOnParcel(session)) {
            return
        }

        val customParcelerTypes = getCustomParcelerTypes(
            property.annotations + containingClassSymbol.resolvedAnnotationsWithClassIds, session
        )
        val source = property.returnTypeRef.source
        val unsubstitutedType = UnsubstitutedTypeWithOrigin(type, origin = null)
        val unsupported = checkParcelableType(type, ParcelizeTypeCheckState(context, customParcelerTypes, source, unsubstitutedType))
        // A property type which is unsupported as a whole is reported with a single diagnostic, which suggests adding '@RawValue'.
        // That suggestion does not apply to recursive data classes, nor to types which already have '@RawValue', so these are
        // reported by the loop below instead, like the unsupported types inside the property type.
        val isUnsupportedType = unsupported.any { it.type == type && it.reason != UnsupportedType.Reason.RECURSIVE_DATA_CLASS }
        if (isUnsupportedType && !type.hasAnyAnnotation(ParcelizeNames.RAW_VALUE_ANNOTATION_FQ_NAMES, session)) {
            reporter.reportOn(source, KtErrorsParcelize.PARCELABLE_TYPE_NOT_SUPPORTED, context)
            return
        }
        for (unsupportedType in unsupported) {
            when (unsupportedType.reason) {
                UnsupportedType.Reason.NOT_SUPPORTED ->
                    reporter.reportOn(source, KtErrorsParcelize.PARCELABLE_TYPE_CONTAINS_NOT_SUPPORTED, unsupportedType.type, context)
                UnsupportedType.Reason.INACCESSIBLE_DATA_CLASS -> {
                    val dataClassType = unsupportedType.type
                        .withNullability(nullable = false, session.typeContext)
                        .withAttributes(ConeAttributes.Empty)
                    reporter.reportOn(source, KtErrorsParcelize.PARCELABLE_TYPE_CONTAINS_NOT_SUPPORTED, dataClassType, context)
                }
                UnsupportedType.Reason.RECURSIVE_DATA_CLASS ->
                    reporter.reportOn(
                        source,
                        KtErrorsParcelize.PARCELABLE_RECURSIVE_DATA_CLASS_NOT_SUPPORTED,
                        unsupportedType.type,
                        context
                    )
            }
        }
    }

    private data class UnsupportedType(val type: ConeKotlinType, val reason: Reason) {
        enum class Reason {
            NOT_SUPPORTED, // The type cannot be serialized by 'Parcelize'. '@RawValue' allows to serialize it with 'writeValue'.

            // The type is a data class which is serialized property by property, but its primary constructor or properties
            // are not accessible. '@RawValue' does not change that.
            INACCESSIBLE_DATA_CLASS,
            RECURSIVE_DATA_CLASS, // The type is a data class which (transitively) contains itself.
        }
    }

    private fun getCustomParcelerTypes(annotations: List<FirAnnotation>, session: FirSession): Set<ConeKotlinType> =
        annotations.mapNotNullTo(mutableSetOf()) { annotation ->
            val resolvedAnnotation = annotation.resolvedType.fullyExpandedType(session)
            if (annotation.fqName(session) in ParcelizeNames.TYPE_PARCELER_FQ_NAMES && resolvedAnnotation.typeArguments.size == 2) {
                resolvedAnnotation.typeArguments[0].type?.fullyExpandedType(session)
            } else {
                null
            }
        }

    private data class ParcelizeTypeCheckState(
        val context: CheckerContext,
        val customParcelerTypes: Set<ConeKotlinType>,
        val source: KtSourceElement?,
        val unsubstitutedType: UnsubstitutedTypeWithOrigin,
        val inDataClass: Boolean = false,
        val fromDataClassProperty: Boolean = false,
        val inRawValue: Boolean = false,
    )

    private class UnsubstitutedTypeWithOrigin(val type: ConeKotlinType, val origin: DataClassExpansion?) {
        // Replaces a type parameter of the origin data class with its type argument
        fun resolve(): UnsubstitutedTypeWithOrigin {
            val typeParameter = (type as? ConeTypeParameterType)?.lookupTag?.typeParameterSymbol ?: return this
            return origin?.typeArguments?.get(typeParameter)?.resolve() ?: this
        }
    }

    private class DataClassExpansion(
        val type: ConeClassLikeType,
        val origin: DataClassExpansion?,
        val typeArguments: Map<FirTypeParameterSymbol, UnsubstitutedTypeWithOrigin>,
    )

    // Returns the set of types that are *not* supported. This set can include types other than `type`
    // if it is a generally supported container type that contains unsupported elements in this instantiation.
    private fun checkParcelableType(type: ConeKotlinType, state: ParcelizeTypeCheckState): Set<UnsupportedType> {
        val (context, customParcelerTypes, inDataClass, unsubstitutedType, inRawValue) = state
        val session = context.session
        if (
            type.hasAnyAnnotation(ParcelizeNames.WRITE_WITH_FQ_NAMES, session) ||
            type in customParcelerTypes ||
            (type.isMarkedNullable && type.withNullability(nullable = false, session.typeContext) in customParcelerTypes)
        ) {
            return emptySet()
        }

        // Unsupported types in a `@RawValue` type are serialized with `writeValue`. Data classes in it are still serialized property
        // by property, though, so they have to meet the corresponding requirements nevertheless.
        if (!inRawValue && type.hasAnyAnnotation(ParcelizeNames.RAW_VALUE_ANNOTATION_FQ_NAMES, session)) {
            return checkParcelableType(type, state.copy(inRawValue = true)).filterTo(mutableSetOf()) {
                it.reason != UnsupportedType.Reason.NOT_SUPPORTED
            }
        }

        val upperBound = type.getErasedUpperBound(session)
        val symbol = upperBound?.toRegularClassSymbol(session)
            ?: return setOf(UnsupportedType(type, UnsupportedType.Reason.NOT_SUPPORTED))

        // The checks below depend on the signature of this class (kind, modifiers, supertypes, constructor).
        if (state.fromDataClassProperty) {
            session.lookupTracker?.recordClassLikeLookup(symbol.classId, state.source, context.containingFileSymbol?.source)
        }

        if (symbol.classKind.isSingleton || symbol.classKind.isEnumClass) {
            return emptySet()
        }

        val fqName = symbol.classId.asFqNameString()
        if (fqName in BuiltinParcelableTypes.PARCELABLE_BASE_TYPE_FQNAMES) {
            return emptySet()
        }

        if (fqName in BuiltinParcelableTypes.PARCELABLE_CONTAINER_FQNAMES) {
            val unsubstitutedTypeArguments = unsubstitutedType.type.getErasedUpperBound(session)?.typeArguments
            return upperBound.typeArguments.foldIndexed(emptySet()) { index, acc, arg ->
                val elementType = arg.type ?: StandardTypes.NullableAny
                val unsubstitutedElementType = unsubstitutedTypeArguments?.getOrNull(index)?.type
                    ?.let { UnsubstitutedTypeWithOrigin(it, unsubstitutedType.origin).resolve() }
                    ?: UnsubstitutedTypeWithOrigin(elementType, unsubstitutedType.origin)
                acc union checkParcelableType(elementType, state.copy(unsubstitutedType = unsubstitutedElementType))
            }
        }

        if (type.anySuperTypeConstructor(session) { it.isParcelableSupertype(session) }) {
            return emptySet()
        }

        if (symbol.isData && (inDataClass || type.customAnnotations.any { it.fqName(session) == ParcelizeNames.DATA_CLASS_ANNOTATION_FQ_NAME })) {
            // A data class which is written in its own declaration, possibly through the declarations of other data classes,
            // is recursive and cannot be serialized inline: its serializer would unfold the same declarations indefinitely.
            // Example: `data class Node(val next: Node?)` or `data class Node<T>(val next: Node<List<T>>?)`.
            // In contrast, a data class which occurs again through its type arguments, such as `Pair` in `Pair<User, Boolean>` with
            // `data class User(val coords: Pair<Int, Int>)`, only unfolds the finite type arguments.
            val recursiveDataClass = generateSequence(unsubstitutedType.origin) { it.origin }
                .firstOrNull { it.type.lookupTag == upperBound.lookupTag }
            if (recursiveDataClass != null) {
                return setOf(UnsupportedType(recursiveDataClass.type, UnsupportedType.Reason.RECURSIVE_DATA_CLASS))
            }

            val properties = symbol.dataClassPrimaryConstructorPropertiesOrNull(session)
                ?: return setOf(UnsupportedType(type, UnsupportedType.Reason.INACCESSIBLE_DATA_CLASS))

            // The code generated for a `@DataClass` type calls the property getters of the data class directly,
            // so it has to be regenerated whenever any of them changes.
            recordDataClassPropertyLookups(symbol, properties, state)

            // Serialization uses the property getters, deserialization uses the constructor.
            if (properties.any { !it.isVisible(context) } || symbol.primaryConstructorIfAny(session)?.isVisible(context) != true) {
                return setOf(UnsupportedType(type, UnsupportedType.Reason.INACCESSIBLE_DATA_CLASS))
            }
            val typeMapping = symbol.typeParameterSymbols.zip(upperBound.typeArguments).mapNotNull { [parameter, arg] ->
                arg.serializedTypeOrNull()?.let { parameter to it }
            }.toMap()
            val substitutor = substitutorByMap(typeMapping, context.session)
            val unsubstitutedTypeArguments = unsubstitutedType.type.getErasedUpperBound(session)?.typeArguments.orEmpty()
            val expansion = DataClassExpansion(
                type = upperBound.withNullability(nullable = false, session.typeContext).withAttributes(ConeAttributes.Empty),
                origin = unsubstitutedType.origin,
                typeArguments = symbol.typeParameterSymbols.zip(unsubstitutedTypeArguments).mapNotNull { [parameter, arg] ->
                    arg.serializedTypeOrNull()?.let { parameter to UnsubstitutedTypeWithOrigin(it, unsubstitutedType.origin).resolve() }
                }.toMap(),
            )
            return properties.fold(emptySet()) { acc, property ->
                val elementType = substitutor.substituteOrSelf(property.resolvedReturnType)
                acc union checkParcelableType(
                    elementType,
                    state.copy(
                        inDataClass = true,
                        fromDataClassProperty = true,
                        unsubstitutedType = UnsubstitutedTypeWithOrigin(property.resolvedReturnType, expansion).resolve(),
                    ),
                )
            }
        }

        if (type.anySuperTypeConstructor(session) { it.isSupportedSerializable() }) {
            return emptySet()
        }

        return setOf(UnsupportedType(type, UnsupportedType.Reason.NOT_SUPPORTED))
    }

    private fun recordDataClassPropertyLookups(
        symbol: FirRegularClassSymbol,
        properties: List<FirPropertySymbol>,
        state: ParcelizeTypeCheckState,
    ) {
        val lookupTracker = state.context.session.lookupTracker ?: return
        val fileSource = state.context.containingFileSymbol?.source
        for (property in properties) {
            lookupTracker.recordClassMemberLookup(property.name.asString(), symbol.classId, state.source, fileSource)
        }
    }

    private fun ConeKotlinType.anySuperTypeConstructor(session: FirSession, predicate: (ConeKotlinType) -> Boolean): Boolean =
        with(session.typeContext) { anySuperTypeConstructor { it is ConeKotlinType && predicate(it) } }

    // The type of the values which are serialized for a type argument, or `null` for star projections and contravariant arguments.
    private fun ConeTypeProjection.serializedTypeOrNull(): ConeKotlinType? =
        when (this) {
            is ConeKotlinType -> this
            is ConeKotlinTypeProjectionOut -> type
            else -> null
        }

    private fun FirCallableSymbol<*>.isVisible(context: CheckerContext): Boolean {
        return context.session.visibilityChecker.isVisible(
            this,
            context.session,
            context.containingFileSymbol ?: return true,
            context.containingDeclarations,
            dispatchReceiver = null
        )
    }

    private fun ConeKotlinType.getErasedUpperBound(session: FirSession): ConeClassLikeType? =
        when (this) {
            is ConeClassLikeType ->
                fullyExpandedClassLikeType(session)

            is ConeTypeParameterType -> {
                val bounds = lookupTag.typeParameterSymbol.resolvedBounds
                val representativeBound = bounds.firstOrNull {
                    val kind = it.coneType.toRegularClassSymbol(session)?.classKind
                        ?: return@firstOrNull false
                    kind != ClassKind.INTERFACE && kind != ClassKind.ANNOTATION_CLASS
                } ?: bounds.first()
                representativeBound.coneType.getErasedUpperBound(session)
            }

            else ->
                null
        }

    private fun ConeKotlinType.isParcelableSupertype(session: FirSession): Boolean =
        classId?.asFqNameString() in BuiltinParcelableTypes.PARCELABLE_SUPERTYPE_FQNAMES || isSomeFunctionType(session)

    private fun ConeKotlinType.isSupportedSerializable(): Boolean =
        classId?.asFqNameString() in BuiltinParcelableTypes.EXTERNAL_SERIALIZABLE_FQNAMES

    private fun ConeKotlinType.hasAnyAnnotation(fqNames: List<FqName>, session: FirSession): Boolean =
        customAnnotations.any { it.fqName(session) in fqNames }

    private fun FirProperty.hasIgnoredOnParcel(session: FirSession): Boolean {
        return annotations.hasIgnoredOnParcel(session) || (getter?.annotations?.hasIgnoredOnParcel(session) ?: false)
    }

    private fun List<FirAnnotation>.hasIgnoredOnParcel(session: FirSession): Boolean {
        return this.any {
            it.fqName(session) in IGNORED_ON_PARCEL_FQ_NAMES
        }
    }

    private fun FirRegularClassSymbol.hasCustomParceler(session: FirSession): Boolean {
        val companionObjectSymbol = this.resolvedCompanionObjectSymbol ?: return false
        return lookupSuperTypes(companionObjectSymbol, lookupInterfaces = true, deep = true, session).any {
            it.classId == PARCELER_ID
        }
    }
}
