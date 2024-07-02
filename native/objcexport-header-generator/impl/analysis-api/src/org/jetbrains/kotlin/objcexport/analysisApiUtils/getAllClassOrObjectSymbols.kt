/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFileSymbol


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
internal fun KaSession.getAllClassOrObjectSymbols(file: KaFileSymbol): List<KaClassSymbol> {
    return file.fileScope.classifiers
        .filterIsInstance<KaClassSymbol>()
        .flatMap { classSymbol ->
            if (isVisibleInObjC(classSymbol)) listOf(classSymbol) + getAllClassOrObjectSymbols(classSymbol)
            else emptyList()
        }
        .toList()
}

private fun KaSession.getAllClassOrObjectSymbols(symbol: KaClassSymbol): Sequence<KaClassSymbol> {
    return sequence {
        val nestedClasses = symbol.memberScope.classifiers.filterIsInstance<KaClassSymbol>()
        yieldAll(nestedClasses)

        nestedClasses.forEach { nestedClass ->
            yieldAll(getAllClassOrObjectSymbols(nestedClass))
        }
    }
}
