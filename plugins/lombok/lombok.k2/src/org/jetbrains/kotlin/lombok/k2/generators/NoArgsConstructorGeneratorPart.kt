/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.k2.generators

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.java.declarations.FirJavaField
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.lombok.k2.config.ConeLombokAnnotations.NoArgsConstructor

class NoArgsConstructorGeneratorPart(session: FirSession) : AbstractConstructorGeneratorPart<NoArgsConstructor>(session) {
    override fun getConstructorInfo(classSymbol: FirClassSymbol<*>): NoArgsConstructor? {
        return lombokService.getNoArgsConstructor(classSymbol)
    }

    override fun getFieldsForParameters(classSymbol: FirClassSymbol<*>): List<FirJavaField> {
        return emptyList()
    }
}
