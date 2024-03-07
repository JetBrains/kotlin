package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.tooling.core.linearClosure

/**
 * Property needs to be translated and support special naming in 2 cases:
 * 1. It has no receiver type and no extension
 * 2. It is a property of an inner class
 *
 * See K1 [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportMapperKt.isObjCProperty]
 */
context(KtAnalysisSession)
internal val KtPropertySymbol.isObjCProperty: Boolean
    get() {
        val hasReceiver = receiverParameter != null && !isExtension
        return !hasReceiver && !isPropertyInInnerClass
    }

context(KtAnalysisSession)
private val KtPropertySymbol.isPropertyInInnerClass: Boolean
    get() = linearClosure<KtSymbol> { symbol -> symbol.getContainingSymbol() }
        .any { it is KtNamedClassOrObjectSymbol && it.isInner }
