package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCClass
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCProtocol

/**
 * Collects symbols which used, but aren't explicitly defined in the source code. For example StringBuilder, Iterator, Array etc.
 */
class KtObjCDependencies {

    private val translatedFqNames = mutableSetOf<String>()

    val protocols = mutableListOf<ObjCProtocol>()
    val classes = mutableListOf<ObjCClass>()

    context(KtAnalysisSession, KtObjCExportSession)
    fun collect(symbol: KtClassOrObjectSymbol) {

        val fqName = symbol.classIdIfNonLocal?.asFqNameString()

        if (symbol.isDeclaredInModule) return
        if (fqName == null) return

        if (translatedFqNames.add(fqName)) {
            symbol.translateToObjCExportStubs().forEach { stub ->
                when (stub) {
                    is ObjCProtocol -> protocols.add(stub)
                    is ObjCClass -> classes.add(stub)
                    else -> Unit
                }
            }
        }
    }
}

context(KtAnalysisSession)
internal val KtClassOrObjectSymbol.isDeclaredInModule: Boolean
    get() = this.getContainingModule().directRegularDependencies.isNotEmpty()