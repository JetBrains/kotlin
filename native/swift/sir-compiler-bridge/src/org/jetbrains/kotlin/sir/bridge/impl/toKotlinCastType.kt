/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.bridge.impl

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.typeParameters
import org.jetbrains.kotlin.sir.SirNominalType
import org.jetbrains.kotlin.sir.SirType
import org.jetbrains.kotlin.sir.bridge.SirTypeNamer
import org.jetbrains.kotlin.sir.providers.source.kaSymbolOrNull

@OptIn(KaExperimentalApi::class)
internal fun SirType.toKotlinCastType(typeNamer: SirTypeNamer): String {
    val fqName = typeNamer.kotlinFqName(this)
    val params = (this as? SirNominalType)?.typeDeclaration?.kaSymbolOrNull<KaClassLikeSymbol>()?.typeParameters?.size ?: 0
    return if (params > 0) {
        "$fqName${translateTypeParameters()}"
    } else fqName
}

/**
 * Generics has limited support only.
 * Full support should be implemented with KT-75546
 */
private fun translateTypeParameters(): String {
    return "<*>"
}