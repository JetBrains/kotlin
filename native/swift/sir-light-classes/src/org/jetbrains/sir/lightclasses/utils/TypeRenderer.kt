/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.utils

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.renderer.types.KaTypeRenderer
import org.jetbrains.kotlin.analysis.api.renderer.types.impl.KaTypeRendererForSource
import org.jetbrains.kotlin.analysis.api.renderer.types.renderers.KaTypeParameterTypeRenderer
import org.jetbrains.kotlin.analysis.api.renderer.types.renderers.KaTypeProjectionRenderer
import org.jetbrains.kotlin.analysis.api.types.KaStarTypeProjection
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeArgumentWithVariance
import org.jetbrains.kotlin.analysis.api.types.KaTypeParameterType
import org.jetbrains.kotlin.analysis.api.types.KaTypeProjection
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter

internal val KaTypeRendererForSource.UPPER_BOUNDS_WITH_QUALIFIED_NAMES: KaTypeRenderer
    get() = WITH_QUALIFIED_NAMES.with {
        typeParameterTypeRenderer = UpperBoundTypeParameterTypeRenderer
        typeProjectionRenderer = UpperBoundTypeProjectionRenderer
    }

private object UpperBoundTypeParameterTypeRenderer : KaTypeParameterTypeRenderer {
    override fun renderType(
        analysisSession: KaSession,
        type: KaTypeParameterType,
        typeRenderer: KaTypeRenderer,
        printer: PrettyPrinter
    ) {
        val type = type.symbol.upperBounds.singleOrNull() ?: analysisSession.builtinTypes.nullableAny
        typeRenderer.renderType(analysisSession, type, printer)
    }
}

private object UpperBoundTypeProjectionRenderer : KaTypeProjectionRenderer {
    override fun renderTypeProjection(
        analysisSession: KaSession,
        projection: KaTypeProjection,
        typeRenderer: KaTypeRenderer,
        printer: PrettyPrinter,
    ) {
        when (projection) {
            is KaStarTypeProjection -> printer.append('*')
            is KaTypeArgumentWithVariance -> {
                var type: KaType? = projection.type
                while (type is KaTypeParameterType) {
                    type = type.symbol.upperBounds.singleOrNull()
                }
                if (type == null) {
                    type = analysisSession.builtinTypes.nullableAny
                }
                typeRenderer.renderType(analysisSession, type, printer)
            }
        }
    }
}
