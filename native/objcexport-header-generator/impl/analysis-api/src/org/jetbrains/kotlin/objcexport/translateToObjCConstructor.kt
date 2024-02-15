package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.backend.konan.descriptors.arrayTypes
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCInstanceType
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCMethod
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCParameter
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCRawType
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getSuperClassSymbolNotAny
import org.jetbrains.kotlin.objcexport.analysisApiUtils.hasExportForCompilerAnnotation
import org.jetbrains.kotlin.objcexport.analysisApiUtils.isVisibleInObjC

context(KtAnalysisSession, KtObjCExportSession)
fun KtClassOrObjectSymbol.translateToObjCConstructors(): List<ObjCMethod> {
    val result = mutableListOf<ObjCMethod>()

    /* Translate declared constructors */
    result += getDeclaredMemberScope().getConstructors()
        .filter { !it.hasExportForCompilerAnnotation }
        .filter { it.isVisibleInObjC() }
        .map { it.buildObjCMethod() }

    /* Create special 'alloc' constructors */
    if (this.classIdIfNonLocal?.asFqNameString() in arrayTypes ||
        classKind.isObject || classKind == KtClassKind.ENUM_CLASS
    ) {
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

    // Hide "unimplemented" super constructors:
    getSuperClassSymbolNotAny()?.getMemberScope()?.getConstructors().orEmpty()
        .filter { it.isVisibleInObjC() }
        .forEach { superClassConstructor ->
            val translatedSuperClassConstructor = superClassConstructor.buildObjCMethod(unavailable = true)
            if (result.none { it.name == translatedSuperClassConstructor.name }) {
                result.add(translatedSuperClassConstructor)
            }
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
