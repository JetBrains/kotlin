/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.fir.checkers

import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.type.FirResolvedTypeRefChecker
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.customAnnotations
import org.jetbrains.kotlinx.serialization.compiler.fir.getSerializableWith
import org.jetbrains.kotlinx.serialization.compiler.fir.hasSerializableOrMetaAnnotation

/**
 * `@Serializable(with = ...)` written on a *type* is only honoured for the types of serializable properties, where
 * [FirSerializationPluginClassChecker] picks it up and the backend passes the serializer on. Anywhere else — a function
 * signature, a local variable, a top-level property — it is silently ignored: `serializer<T>()` and the reflective
 * lookup do not see type annotations. Code like
 *
 * ```
 * fun fromString(value: String): List<@Serializable(with = ClassASerializer::class) ClassA> =
 *     Json.decodeFromString(value)
 * ```
 *
 * used to work by accident before 2.0 and now quietly decodes with the wrong serializer. See KT-69488.
 */
object FirSerializationTypeRefChecker : FirResolvedTypeRefChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(typeRef: FirResolvedTypeRef) {
        // Nested type arguments are visited on their own, so only the annotations of this very type matter here.
        if (typeRef.coneType.customAnnotations.getSerializableWith(context.session) == null) return
        // Types inferred for a call, e.g. the type argument of `decodeFromString`, inherit the annotations of the
        // expected type. Those are not places the user can fix, so only report where the annotation is written.
        if (typeRef.source?.kind !is KtRealSourceElementKind) return
        if (isHonouredPosition()) return

        reporter.reportOn(typeRef.source, FirSerializationErrors.SERIALIZABLE_WITH_ON_TYPE_HAS_NO_EFFECT)
    }

    context(context: CheckerContext)
    private fun isHonouredPosition(): Boolean {
        val containingClassSymbol = when (val innermost = context.containingDeclarations.lastOrNull()) {
            // `typealias S = @Serializable(SomeSerializer::class) Other` is the documented way to bind a serializer to
            // a type one does not own: the annotation survives the expansion and is picked up at every use site.
            is FirTypeAliasSymbol -> return true
            is FirPropertySymbol -> innermost.getContainingClassSymbol()
            is FirValueParameterSymbol ->
                (innermost.containingDeclarationSymbol as? FirConstructorSymbol)
                    ?.takeIf { it.isPrimary }
                    ?.getContainingClassSymbol()
            else -> null
        }
        return (containingClassSymbol as? FirClassSymbol<*>)?.hasSerializableOrMetaAnnotation(context.session) == true
    }
}
