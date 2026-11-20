/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize.fir

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.declaredProperties
import org.jetbrains.kotlin.fir.declarations.primaryConstructorIfAny
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.fir.declarations.utils.fromPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.utils.isData
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.resolve.lookupSuperTypes
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.parcelize.ParcelizeNames.IGNORED_ON_PARCEL_CLASS_IDS
import org.jetbrains.kotlin.parcelize.ParcelizeNames.PARCELER_CLASS_IDS
import org.jetbrains.kotlin.parcelize.ParcelizeNames.POLYMORPHIC_SEALED_CLASS_IDS
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
fun FirClassSymbol<*>?.isParcelize(session: FirSession): Boolean {
    contract {
        returns(true) implies (this@isParcelize != null)
    }

    if (this == null) return false
    return checkParcelizeClassSymbols(this, session) { it.hasParcelizeAnnotation(session) }
}

/**
 * Returns `true` if this class itself is annotated with one of the `Parcelize` annotations.
 * Unlike [isParcelize], sealed supertypes are not taken into account.
 */
fun FirClassSymbol<*>.hasParcelizeAnnotation(session: FirSession): Boolean {
    return hasAnyAnnotation(session.parcelizeService.parcelizeAnnotations, session)
}

fun FirBasedSymbol<*>.hasAnyAnnotation(classIds: Collection<ClassId>, session: FirSession): Boolean {
    return resolvedAnnotationsWithClassIds.any { it.toAnnotationClassId(session) in classIds }
}

/**
 * Returns the first `@IgnoredOnParcel` annotation, either from the current or from the deprecated runtime package,
 * or `null` if there is none.
 */
fun List<FirAnnotation>.findIgnoredOnParcel(session: FirSession): FirAnnotation? {
    return firstOrNull { it.toAnnotationClassId(session) in IGNORED_ON_PARCEL_CLASS_IDS }
}

/**
 * Check all related [FirClassSymbol]s to the provided [symbol] which are valid locations for a
 * `Parcelize` annotation to be present. This commonizes class symbol navigation between checker and
 * generator, even though [predicate] implementation is different.
 */
inline fun checkParcelizeClassSymbols(
    symbol: FirClassSymbol<*>,
    session: FirSession,
    predicate: (FirClassSymbol<*>) -> Boolean,
): Boolean {
    if (predicate(symbol)) return true
    return symbol.resolvedSuperTypeRefs.any { superTypeRef ->
        val superTypeSymbol = superTypeRef.coneType.toRegularClassSymbol(session)
            ?.takeIf { it.rawStatus.modality == Modality.SEALED }
            ?: return@any false
        predicate(superTypeSymbol)
    }
}

/**
 * Returns `true` if the companion object of this class implements `Parceler`, either from the current or from the deprecated runtime
 * package. Such classes provide their own serialization logic.
 */
fun FirRegularClassSymbol.hasCustomParceler(session: FirSession): Boolean {
    val companion = resolvedCompanionObjectSymbol ?: return false
    return lookupSuperTypes(companion, lookupInterfaces = true, deep = true, useSiteSession = session).any {
        it.classId in PARCELER_CLASS_IDS
    }
}

/**
 * Returns `true` if this class is annotated with `@PolymorphicSealed`.
 * Unlike `isPolymorphicSealed` in the IR part of the plugin, this function does not check that the class is actually `sealed`,
 * as this is reported by a dedicated checker.
 */
fun FirClassLikeSymbol<*>.hasPolymorphicSealedAnnotation(session: FirSession): Boolean {
    return hasAnyAnnotation(POLYMORPHIC_SEALED_CLASS_IDS, session)
}

/**
 * Returns the properties declared by the primary constructor of a data class, in the order of the constructor's value parameters,
 * or `null` if the primary constructor or any of its corresponding properties cannot be found.
 * The properties are matched to the primary constructor's value parameters by name, since [fromPrimaryConstructor] is only set for classes
 * built from sources and is absent for deserialized classes.
 */
fun FirRegularClassSymbol.dataClassPrimaryConstructorPropertiesOrNull(session: FirSession): List<FirPropertySymbol>? {
    require(isData) { "This function is to be used for data classes only" }

    val primaryConstructor = primaryConstructorIfAny(session) ?: return null
    val memberProperties = declaredProperties(session).filter {
        it.receiverParameterSymbol == null && it.contextParameterSymbols.isEmpty()
    }
    return primaryConstructor.valueParameterSymbols.map { parameter ->
        memberProperties.singleOrNull { it.name == parameter.name } ?: return null
    }
}
