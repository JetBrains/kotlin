/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.fir.checkers

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlinx.serialization.compiler.fir.*
import org.jetbrains.kotlinx.serialization.compiler.fir.getSerializableWith
import org.jetbrains.kotlinx.serialization.compiler.fir.getSerializerAnnotation
import org.jetbrains.kotlinx.serialization.compiler.fir.getSerializerForClass

// Extracted from FirSerializationPluginClassChecker to keep it reasonably small
internal fun CheckerContext.checkCompanionOfSerializableClass(
    classSymbol: FirClassSymbol<*>,
    reporter: DiagnosticReporter,
) {
    if (classSymbol !is FirRegularClassSymbol) return
    val companionObjectSymbol = classSymbol.companionObjectSymbol ?: return
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
        classSymbol,
        this
    )
}

internal fun CheckerContext.checkCompanionSerializerDependency(
    classSymbol: FirClassSymbol<*>,
    reporter: DiagnosticReporter,
) {
    if (classSymbol !is FirRegularClassSymbol) return
    val companionObjectSymbol = classSymbol.companionObjectSymbol ?: return
    val serializerForInCompanion = companionObjectSymbol.getSerializerForClass(session)?.toRegularClassSymbol(session) ?: return
    val serializableWith: ConeKotlinType? = classSymbol.getSerializableWith(session)
    val context = this@checkCompanionSerializerDependency

    fun reportSerializableCompanion() {
        reporter.reportOn(
            companionObjectSymbol.getSerializerAnnotation(session)?.source,
            FirSerializationErrors.COMPANION_OBJECT_SERIALIZER_INSIDE_OTHER_SERIALIZABLE_CLASS,
            classSymbol.defaultType(),
            serializerForInCompanion.defaultType(),
            context
        )
    }

    return when {
        classSymbol.hasSerializableOrMetaAnnotationWithoutArgs(session) -> {
            if (serializerForInCompanion.classId == classSymbol.classId) {
                // @Serializable class Foo / @Serializer(Foo::class) companion object — prohibited due to problems with recursive resolve
                reporter.reportOn(
                    classSymbol.serializableOrMetaAnnotationSource(session),
                    FirSerializationErrors.COMPANION_OBJECT_AS_CUSTOM_SERIALIZER_DEPRECATED,
                    classSymbol,
                    context
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
                serializerForInCompanion.defaultType(),
                context
            )
        }
    }
}
