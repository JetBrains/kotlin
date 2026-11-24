/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.scopes.fileScope
import org.jetbrains.kotlin.analysis.api.scopes.memberScope
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFileSymbol
import org.jetbrains.kotlin.tooling.core.withClosure


/**
 * Returns all classes or objects (including transitively nested ones) contained within a file
 *
 * Example:
 *```kotlin
 * class A {
 *   class B {
 *     class C
 *   }
 * }
 *```
 * returns `sequenceOf(A, B, C)`
 *
 * See K1 [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportHeaderGenerator.collectClasses]
 */
context(session: KaSession)
internal fun getAllVisibleInObjClassifiers(file: KaFileSymbol): List<KaClassSymbol> {
    return getAllVisibleInObjClassifiers(file.fileScope.classifiers.filterIsInstance<KaClassSymbol>())
}

context(session: KaSession)
internal fun getAllVisibleInObjClassifiers(symbols: Sequence<KaClassSymbol>): List<KaClassSymbol> {
    return getAllVisibleInObjClassifiers(symbols.toList())
}

context(session: KaSession)
internal fun getAllVisibleInObjClassifiers(symbols: Iterable<KaClassSymbol>): List<KaClassSymbol> {
    return symbols.withClosure<KaClassSymbol> { symbol ->
        if (isVisibleInObjC(symbol)) {
            symbol.memberScope.classifiers.filterIsInstance<KaClassSymbol>().asIterable()
        } else {
            emptyList()
        }
    }.toList()
}
