/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.fir.checkers

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirTypeAliasChecker
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationAnnotations

/**
 * `@Serializable` is processed on the COMPILER_REQUIRED_ANNOTATIONS phase, which does not expand annotation type
 * aliases. Code like
 *
 * ```
 * typealias Serializable = kotlinx.serialization.Serializable
 *
 * @Serializable
 * class Some
 * ```
 *
 * therefore compiles, but the class is silently not processed by the plugin — it used to work in K1. Warn on the alias
 * and point at `@MetaSerializable`, which is the supported way to introduce an own name for `@Serializable`.
 *
 * Only `@Serializable` is reported. The remaining serialization annotations are consumed by the backend, which does
 * expand aliases correctly. See KT-69067.
 */
object FirSerializationTypeAliasChecker : FirTypeAliasChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirTypeAlias) {
        val expandedClassId = declaration.expandedTypeRef.coneType.fullyExpandedType().classId ?: return
        if (expandedClassId != SerializationAnnotations.serializableAnnotationClassId) return

        reporter.reportOn(declaration.source, FirSerializationErrors.SERIALIZABLE_ANNOTATION_TYPEALIAS_UNSUPPORTED)
    }
}
