/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.fir.checkers

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.declaredFunctions
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlinx.serialization.compiler.fir.*
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializersClassIds

// Extracted from FirSerializationPluginClassChecker to keep it reasonably small
internal fun CheckerContext.checkCompanionOfSerializableClass(
    classSymbol: FirClassSymbol<*>,
    reporter: DiagnosticReporter,
) {
    if (classSymbol !is FirRegularClassSymbol) return
    val companionObjectSymbol = classSymbol.resolvedCompanionObjectSymbol ?: return
    if (!classSymbol.hasSerializableOrMetaAnnotation(session)) return
    if (!companionObjectSymbol.hasSerializableOrMetaAnnotation(session)) return
    val serializableArg = classSymbol.getSerializableWith(session)
    val companionArg = companionObjectSymbol.getSerializableWith(session)
    if (serializableArg != null && companionArg != null && serializableArg.classId == companionArg.classId) {
        // allowed
        return
    }
    // other versions are not allowed
    reporter.reportOn(
        companionObjectSymbol.serializableOrMetaAnnotationSource(session),
        FirSerializationErrors.COMPANION_OBJECT_IS_SERIALIZABLE_INSIDE_SERIALIZABLE_CLASS,
        classSymbol
    )
}

internal fun CheckerContext.checkPrivateCompanion(
    classSymbol: FirClassSymbol<*>,
    reporter: DiagnosticReporter,
) {
    if (classSymbol !is FirRegularClassSymbol) return
    if (!classSymbol.shouldHaveGeneratedMethodsInCompanion(session)) return
    if (classSymbol.visibility == Visibilities.Private || classSymbol.visibility == Visibilities.Internal) return
    val companionObjectSymbol = classSymbol.resolvedCompanionObjectSymbol ?: return
    if (companionObjectSymbol.visibility != Visibilities.Private) return

    reporter.reportOn(
        companionObjectSymbol.source,
        FirSerializationErrors.PRIVATE_COMPANION_OF_SERIALIZABLE,
        classSymbol,
        positioningStrategy = SourceElementPositioningStrategies.VISIBILITY_MODIFIER
    )
}

/**
 * The signature predicate here must stay in sync with
 * `SerializableCompanionIrGenerator.getSerializerGetterFunction`, which is what actually picks the
 * function to generate a body for in the backend.
 */
internal fun CheckerContext.checkCompanionSerializerClash(
    classSymbol: FirClassSymbol<*>,
    reporter: DiagnosticReporter,
) {
    if (classSymbol !is FirRegularClassSymbol) return
    if (!classSymbol.shouldHaveGeneratedMethodsInCompanion(session)) return
    // For a serializable object the backend looks the getter up in the object itself rather than in a companion,
    // see SerializableCompanionIrGenerator.getSerializerGetterFunction.
    val containerSymbol = when {
        classSymbol.isSerializableObject(session) -> classSymbol
        else -> classSymbol.resolvedCompanionObjectSymbol ?: return
    }

    val generatedNames = buildSet {
        add(SerialEntityNames.SERIALIZER_PROVIDER_NAME)
        if (classSymbol.keepGeneratedSerializer(session)) add(SerialEntityNames.GENERATED_SERIALIZER_PROVIDER_NAME)
    }

    for (functionSymbol in containerSymbol.declaredFunctions(session)) {
        if (functionSymbol.name !in generatedNames) continue
        if (functionSymbol.origin != FirDeclarationOrigin.Source) continue
        // The backend matches one serializer parameter per type parameter of the serializable class, counting every
        // parameter but the dispatch receiver — so an extension or context receiver makes the signature not match.
        val nonDispatchParameterTypes = buildList {
            functionSymbol.contextParameterSymbols.mapTo(this) { it.resolvedReturnType }
            functionSymbol.resolvedReceiverType?.let { add(it) }
            functionSymbol.valueParameterSymbols.mapTo(this) { it.resolvedReturnType }
        }
        if (nonDispatchParameterTypes.size != classSymbol.typeParameterSymbols.size) continue
        if (!nonDispatchParameterTypes.all { isAnyKSerializer(it) }) continue
        if (!isAnyKSerializer(functionSymbol.resolvedReturnType)) continue

        reporter.reportOn(
            functionSymbol.source,
            FirSerializationErrors.SERIALIZER_FUNCTION_CLASH_IN_COMPANION,
            functionSymbol.name.asString()
        )
    }
}

private fun CheckerContext.isAnyKSerializer(type: ConeKotlinType): Boolean {
    val expanded = type.fullyExpandedType()
    return expanded.isKSerializer || expanded.classId == SerializersClassIds.generatedSerializerId
}

internal fun CheckerContext.checkCompanionSerializerDependency(
    classSymbol: FirClassSymbol<*>,
    reporter: DiagnosticReporter,
) {
    if (classSymbol !is FirRegularClassSymbol) return
    val companionObjectSymbol = classSymbol.resolvedCompanionObjectSymbol ?: return
    val serializerForInCompanion = companionObjectSymbol.getSerializerForClass(session)?.toRegularClassSymbol() ?: return
    val serializableWith: ConeKotlinType? = classSymbol.getSerializableWith(session)
    val context = this@checkCompanionSerializerDependency

    fun reportSerializableCompanion() {
        reporter.reportOn(
            companionObjectSymbol.getSerializerAnnotation(session)?.source,
            FirSerializationErrors.COMPANION_OBJECT_SERIALIZER_INSIDE_OTHER_SERIALIZABLE_CLASS,
            classSymbol.defaultType(),
            serializerForInCompanion.defaultType()
        )
    }

    return when {
        classSymbol.hasSerializableOrMetaAnnotationWithoutArgs(session) -> {
            if (serializerForInCompanion.classId == classSymbol.classId) {
                // @Serializable class Foo / @Serializer(Foo::class) companion object — prohibited due to problems with recursive resolve
                reporter.reportOn(
                    classSymbol.serializableOrMetaAnnotationSource(session),
                    FirSerializationErrors.COMPANION_OBJECT_AS_CUSTOM_SERIALIZER_DEPRECATED,
                    classSymbol
                )
            } else {
                // @Serializable class Foo / @Serializer(Bar::class) companion object — prohibited as vague and confusing
                reportSerializableCompanion()
            }
        }

        serializableWith != null -> {
            if (serializableWith.classId == companionObjectSymbol.classId && serializerForInCompanion.classId == classSymbol.classId) {
                // @Serializable(Foo.Companion) class Foo / @Serializer(Foo::class) companion object — the only case that is allowed
            } else {
                // @Serializable(anySer) class Foo / @Serializer(anyOtherClass) companion object — prohibited as vague and confusing
                reportSerializableCompanion()
            }
        }

        else -> {
            // (regular) class Foo / @Serializer(something) companion object - not recommended
            reporter.reportOn(
                companionObjectSymbol.getSerializerAnnotation(session)?.source,
                FirSerializationErrors.COMPANION_OBJECT_SERIALIZER_INSIDE_NON_SERIALIZABLE_CLASS,
                classSymbol.defaultType(),
                serializerForInCompanion.defaultType()
            )
        }
    }
}
