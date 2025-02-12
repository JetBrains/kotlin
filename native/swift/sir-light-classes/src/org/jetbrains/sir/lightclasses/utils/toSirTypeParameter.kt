/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.utils

import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.sir.SirGenericType
import org.jetbrains.kotlin.sir.SirParameter
import org.jetbrains.sir.lightclasses.extensions.SirAndKaSession

/**
 * Only single type parameter is currently supported
 * See KT-75546
 */
internal fun SirAndKaSession.toSirTypeParameter(typeParameter: KaTypeParameterSymbol): SirParameter {
    val type = if (typeParameter.upperBounds.isEmpty()) {
        SirGenericType()
    } else {
        typeParameter.upperBounds.first().translateType(
            ktAnalysisSession = useSiteSession,
            reportErrorType = { error("Type translation error") },
            reportUnsupportedType = { error("Unsupported type encountered") },
            processTypeImports = { error("Unexpected import processing") }
        )
    }

    return SirParameter(
        argumentName = typeParameter.name.toString(),
        parameterName = typeParameter.name.toString(),
        type = type
    )
}