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
context(KaSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
internal fun KaFileSymbol.getAllClassOrObjectSymbols(): List<KaClassSymbol> {
    return fileScope.classifiers
        .filterIsInstance<KaClassSymbol>()
        .flatMap { classSymbol ->
            if (classSymbol.isVisibleInObjC()) listOf(classSymbol) + classSymbol.getAllClassOrObjectSymbols()
            else emptyList()
        }
        .toList()
}

context(KaSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
private fun KaClassSymbol.getAllClassOrObjectSymbols(): Sequence<KaClassSymbol> {
    return sequence {
        val nestedClasses = memberScope.classifiers.filterIsInstance<KaClassSymbol>()
        yieldAll(nestedClasses)

        nestedClasses.forEach { nestedClass ->
            yieldAll(nestedClass.getAllClassOrObjectSymbols())
        }
    }
}
