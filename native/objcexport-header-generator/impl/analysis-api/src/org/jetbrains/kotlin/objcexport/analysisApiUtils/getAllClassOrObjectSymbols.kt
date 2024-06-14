package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaClassOrObjectSymbol
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
 */
context(KaSession)
internal fun KaFileSymbol.getAllClassOrObjectSymbols(): List<KaClassOrObjectSymbol> {
    return fileScope.classifiers
        .filterIsInstance<KaClassOrObjectSymbol>()
        .flatMap { classSymbol -> listOf(classSymbol) + classSymbol.getAllClassOrObjectSymbols() }
        .toList()
}

context(KaSession)
private fun KaClassOrObjectSymbol.getAllClassOrObjectSymbols(): Sequence<KaClassOrObjectSymbol> {
    return sequence {
        val nestedClasses = memberScope.classifiers.filterIsInstance<KaClassOrObjectSymbol>()
        yieldAll(nestedClasses)

        nestedClasses.forEach { nestedClass ->
            yieldAll(nestedClass.getAllClassOrObjectSymbols())
        }
    }
}