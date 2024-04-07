package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtClassifierSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtFileSymbol


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
context(KtAnalysisSession)
internal fun KtFileSymbol.getAllClassOrObjectSymbols(): List<KtClassOrObjectSymbol> {
    return getFileScope().getClassifierSymbols()
        .filterIsInstance<KtClassOrObjectSymbol>()
        .flatMap { classSymbol -> listOf(classSymbol) + classSymbol.getAllClassOrObjectSymbols() }
        .toList()
}

context(KtAnalysisSession)
private fun KtClassOrObjectSymbol.getAllClassOrObjectSymbols(): Sequence<KtClassOrObjectSymbol> {
    return sequence {
        val nestedClasses = getMemberScope().getClassifierSymbols().filterIsInstance<KtClassOrObjectSymbol>()
        yieldAll(nestedClasses)

        nestedClasses.forEach { nestedClass ->
            yieldAll(nestedClass.getAllClassOrObjectSymbols())
        }
    }
}