/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.backend.konan.descriptors.arrayTypes
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCInstanceType
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCMethod
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCParameter
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCRawType
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getSuperClassSymbolNotAny
import org.jetbrains.kotlin.objcexport.analysisApiUtils.hasExportForCompilerAnnotation
import org.jetbrains.kotlin.objcexport.analysisApiUtils.isVisibleInObjC

fun ObjCExportContext.translateToObjCConstructors(symbol: KaClassSymbol): List<ObjCMethod> {

    /* Translate declared constructors */
    val result = with(analysisSession) { symbol.declaredMemberScope }
        .constructors
        .filter { !it.hasExportForCompilerAnnotation }
        .filter { analysisSession.isVisibleInObjC(it) }
        .sortedWith(StableCallableOrder)
        .flatMap { constructor ->
            val objCConstructor = buildObjCMethod(constructor)
            listOf(objCConstructor) + if (objCConstructor.name == "init") listOf(analysisSession.buildNewInitConstructor(constructor)) else emptyList()
        }
        .toMutableList()

    /* Create special 'alloc' constructors */
    if (symbol.classId?.asFqNameString() in arrayTypes ||
        symbol.classKind.isObject || symbol.classKind == KaClassKind.ENUM_CLASS
    ) {
        result.add(
            ObjCMethod(
                comment = null,
                origin = null,
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
                origin = analysisSession.getObjCExportStubOrigin(symbol),
                isInstanceMethod = false,
                returnType = ObjCInstanceType,
                selectors = listOf("allocWithZone:"),
                parameters = listOf(ObjCParameter("zone", null, ObjCRawType("struct _NSZone *"), null)),
                attributes = listOf("unavailable")
            )
        )
    }

    // Hide "unimplemented" super constructors:
    with(analysisSession) { this@translateToObjCConstructors.analysisSession.getSuperClassSymbolNotAny(symbol)?.memberScope }?.constructors.orEmpty()
        .filter { analysisSession.isVisibleInObjC(it) }
        .forEach { superClassConstructor ->
            val translatedSuperClassConstructor = buildObjCMethod(superClassConstructor, unavailable = true)
            if (result.none { it.name == translatedSuperClassConstructor.name }) {
                result.add(translatedSuperClassConstructor)
            }
        }

    return result
}

/**
 * Additional primary constructor which goes always after primary constructor ([ObjCMethod.name] == "init")
 */
private fun KaSession.buildNewInitConstructor(constructor: KaFunctionSymbol): ObjCMethod {
    return ObjCMethod(
        comment = null,
        origin = getObjCExportStubOrigin(constructor),
        isInstanceMethod = false,
        returnType = ObjCInstanceType,
        selectors = listOf("new"),
        parameters = emptyList(),
        attributes = listOf(
            "availability(swift, unavailable, message=\"use object initializers instead\")"
        )
    )
}
