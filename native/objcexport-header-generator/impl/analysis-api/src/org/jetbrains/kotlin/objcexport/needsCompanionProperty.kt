package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.objcexport.analysisApiUtils.isCompanion

/**
 * [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportTranslator.needCompanionObjectProperty]
 */
context(KtAnalysisSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
internal val KtClassOrObjectSymbol.needsCompanionProperty: Boolean
    get() {
        return this.getStaticMemberScope().getClassifierSymbols()
            .any { (it as? KtClassOrObjectSymbol)?.isCompanion == true }
    }