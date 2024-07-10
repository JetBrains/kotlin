package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportStub
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCInstanceType
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCMethod
import org.jetbrains.kotlin.backend.konan.objcexport.swiftNameAttribute

private val initAttribute = swiftNameAttribute("init()")

/**
 * Returns init method in 2 cases:
 * 1. If [symbol] is object
 * 2. If [symbol] is not object and if [members] do not include `init` already
 * ```
 * + (instancetype)foo __attribute__((swift_name("init()")));
 * ```
 */
internal fun ObjCExportContext.addInitIfNeeded(symbol: KaClassSymbol, members: List<ObjCExportStub>): ObjCMethod? {

    val hasInit = members.any { it is ObjCMethod && it.attributes.contains(initAttribute) }
    val isObject = symbol.classKind == KaClassKind.OBJECT

    return if (!hasInit || isObject) {
        ObjCMethod(
            comment = null,
            origin = null,
            isInstanceMethod = false,
            returnType = ObjCInstanceType,
            selectors = listOf(getObjectInstanceSelector(symbol)),
            parameters = emptyList(),
            attributes = listOf(initAttribute),
        )
    } else {
        null
    }
}