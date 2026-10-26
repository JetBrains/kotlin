/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.declaredProperties
import org.jetbrains.kotlin.fir.declarations.primaryConstructorIfAny
import org.jetbrains.kotlin.fir.declarations.utils.fromPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.utils.isData
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol

/**
 * Returns the properties declared by the primary constructor of a data class, in the order of the constructor's value parameters,
 * or `null` if the primary constructor or any of its corresponding properties cannot be found.
 * The properties are matched to the primary constructor's value parameters by name, since [fromPrimaryConstructor] is only set for classes
 * built from sources and is absent for deserialized classes.
 */
fun FirRegularClassSymbol.dataClassPrimaryConstructorPropertiesOrNull(session: FirSession): List<FirPropertySymbol>? {
    require(isData) { "This function is to be used for data classes only" }

    val primaryConstructor = primaryConstructorIfAny(session) ?: return null
    val memberProperties = declaredProperties(session).filter {
        it.receiverParameterSymbol == null && it.contextParameterSymbols.isEmpty()
    }
    return primaryConstructor.valueParameterSymbols.map { parameter ->
        memberProperties.singleOrNull { it.name == parameter.name } ?: return null
    }
}
