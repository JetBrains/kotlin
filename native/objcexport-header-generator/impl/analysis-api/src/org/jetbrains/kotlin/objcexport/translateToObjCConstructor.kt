package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtConstructorSymbol
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCInstanceType
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCMethod
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCParameter
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCRawType
import org.jetbrains.kotlin.objcexport.analysisApiUtils.isArrayConstructor
import org.jetbrains.kotlin.objcexport.analysisApiUtils.isVisibleInObjC

context(KtAnalysisSession, KtObjCExportSession)
fun KtConstructorSymbol.translateToObjCConstructors(): List<ObjCMethod> {
    if (!isVisibleInObjC()) return emptyList()
    val result = mutableListOf<ObjCMethod>()

    result.add(buildObjCMethod())

    if (isArrayConstructor) {

        result.add(
            ObjCMethod(
                comment = null,
                origin = getObjCExportStubOrigin(),
                isInstanceMethod = false,
                returnType = ObjCInstanceType,
                selectors = listOf("alloc"),
                parameters = emptyList(),
                attributes = listOf("unavailable")
            )
        )

        result.add(
            ObjCMethod(
                comment = null,
                origin = getObjCExportStubOrigin(),
                isInstanceMethod = false,
                returnType = ObjCInstanceType,
                selectors = listOf("allocWithZone:"),
                parameters = listOf(ObjCParameter("zone", null, ObjCRawType("struct _NSZone *"), null)),
                attributes = listOf("unavailable")
            )
        )
    }

    if (result.size == 1 && result.first().name == "init") {
        result.add(
            ObjCMethod(
                comment = null,
                origin = getObjCExportStubOrigin(),
                isInstanceMethod = false,
                returnType = ObjCInstanceType,
                selectors = listOf("new"),
                parameters = emptyList(),
                attributes = listOf(
                    "availability(swift, unavailable, message=\"use object initializers instead\")"
                )
            )
        )
    }

    return result
}