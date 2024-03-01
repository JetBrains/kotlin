package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.backend.konan.KonanFqNames
import org.jetbrains.kotlin.name.ClassId


internal val KtCallableSymbol.hasThrowsAnnotation: Boolean
    get() {
        return annotationsList.hasAnnotation(ClassId.topLevel(KonanFqNames.throws))
    }