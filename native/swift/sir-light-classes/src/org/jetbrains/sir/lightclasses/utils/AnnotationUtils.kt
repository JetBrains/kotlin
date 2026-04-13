/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.utils

import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.sir.SirAttribute
import org.jetbrains.kotlin.sir.providers.utils.allRequiredOptIns
import org.jetbrains.kotlin.sir.providers.utils.deprecatedAnnotation
import org.jetbrains.sir.lightclasses.SirFromKtSymbol
import org.jetbrains.sir.lightclasses.extensions.withSessions

internal inline val <reified S : KaDeclarationSymbol> SirFromKtSymbol<S>.translatedAttributes: List<SirAttribute>
    get() = buildList {
        translateAvailabilityAttribute?.let(::add)
        addAll(translatedOptInAttributes)
    }

internal inline val <reified S : KaDeclarationSymbol> SirFromKtSymbol<S>.translatedOptInAttributes: List<SirAttribute>
    get() = withSessions {
        ktSymbol.allRequiredOptIns.mapNotNull {
            if (it.asFqNameString() == "kotlinx.cinterop.BetaInteropApi") return@mapNotNull null
            SirAttribute.SPI(it.asSingleFqName().pathSegments().joinToString("$"))
        }
    }

internal inline val <reified S : KaDeclarationSymbol> SirFromKtSymbol<S>.translateAvailabilityAttribute: SirAttribute?
    get() = ktSymbol.deprecatedAnnotation?.takeIf { it.level != DeprecationLevel.HIDDEN }?.let { annotation ->
        SirAttribute.Available(
            message = annotation.replaceWith.expression.takeIf { it.isNotBlank() }?.let { "${annotation.message}. Replacement: $it" }
                ?: annotation.message,
            deprecated = annotation.level == DeprecationLevel.WARNING,
            unavailable = annotation.level == DeprecationLevel.ERROR,
        )
    }
