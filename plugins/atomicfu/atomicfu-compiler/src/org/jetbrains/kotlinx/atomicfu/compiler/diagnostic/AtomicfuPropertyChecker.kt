/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.diagnostic

import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.utils.effectiveVisibility
import org.jetbrains.kotlin.fir.declarations.utils.isCompanionBlockMember
import org.jetbrains.kotlin.fir.declarations.utils.isCompanionExtension
import org.jetbrains.kotlin.fir.resolve.transformers.publishedApiEffectiveVisibility
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.text

private const val KOTLINX_ATOMICFU = "kotlinx.atomicfu"

private fun FirProperty.isKotlinxAtomicfu(): Boolean = returnTypeRef.coneType.classId?.packageFqName?.asString() == KOTLINX_ATOMICFU

private val FirProperty.resolvedVisibility: EffectiveVisibility
    get() = publishedApiEffectiveVisibility ?: effectiveVisibility

object AtomicfuPropertyChecker : FirPropertyChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirProperty) {
        if (!declaration.isKotlinxAtomicfu()) return
        if (!declaration.resolvedVisibility.privateApi &&
            (declaration.isCompanionBlockMember || declaration.isCompanionExtension)
        ) {
            // Companion block's properties and companion extension properties will be supported in the plugin
            // at the same exact time they become available for our users,
            // and it's a chance to make things right from the beginning and support only private properties.
            reporter.reportOn(
                declaration.source,
                AtomicfuErrors.NON_PRIVATE_ATOMIC_COMPANIONS_ARE_FORBIDDEN,
                declaration.source.text.toString()
            )
        } else if (!declaration.effectiveVisibility.publicApi && declaration.resolvedVisibility.publicApi) {
            reporter.reportOn(
                declaration.source,
                AtomicfuErrors.PUBLISHED_API_ATOMICS_ARE_FORBIDDEN,
                declaration.source.text.toString()
            )
        } else if (declaration.resolvedVisibility.publicApi) {
            reporter.reportOn(
                declaration.source,
                AtomicfuErrors.PUBLIC_ATOMICS_ARE_FORBIDDEN,
                declaration.source.text.toString()
            )
        }
        if (declaration.isVar) {
            reporter.reportOn(
                declaration.source,
                AtomicfuErrors.ATOMIC_PROPERTIES_SHOULD_BE_VAL,
                declaration.source.text.toString()
            )
        }
    }
}
