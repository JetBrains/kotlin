package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaClassOrObjectSymbol
import org.jetbrains.kotlin.objcexport.analysisApiUtils.isCompanion

/**
 * [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportTranslator.needCompanionObjectProperty]
 */
context(KaSession)
internal val KaClassOrObjectSymbol.needsCompanionProperty: Boolean
    get() {
        return this.staticMemberScope.classifiers
            .any { (it as? KaClassOrObjectSymbol)?.isCompanion == true }
    }