/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.utils

import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.sir.SirAttribute
import org.jetbrains.kotlin.sir.providers.utils.deprecatedAnnotation
import org.jetbrains.sir.lightclasses.SirFromKtSymbol

internal inline val <reified S : KaDeclarationSymbol> SirFromKtSymbol<S>.translatedAttributes
    get(): List<SirAttribute> {
        val availability = ktSymbol.deprecatedAnnotation?.takeIf { it.level != DeprecationLevel.HIDDEN }?.let {
            SirAttribute.Available(
                message = it.message,
                deprecated = it.level == DeprecationLevel.WARNING,
                unavailable = it.level == DeprecationLevel.ERROR,
                renamed = it.replaceWith.expression,
            )
        }

        return listOfNotNull(availability)
    }