/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.export.utilities.getSuperClassSymbolNotAny
import org.jetbrains.kotlin.analysis.api.export.utilities.hasExportForCompilerAnnotation
import org.jetbrains.kotlin.analysis.api.export.utilities.isCompanion
import org.jetbrains.kotlin.analysis.api.scopes.declaredMemberScope
import org.jetbrains.kotlin.analysis.api.scopes.memberScope
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.backend.konan.descriptors.arrayTypes
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCInstanceType
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCMethod
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCParameter
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCRawType
import org.jetbrains.kotlin.objcexport.analysisApiUtils.isVisibleInObjC
import org.jetbrains.kotlin.utils.addIfNotNull

fun ObjCExportContext.translateToObjCConstructors(symbol: KaClassSymbol): List<ObjCMethod> {

    /* Translate declared constructors */
    val result = context(analysisSession) { symbol.declaredMemberScope }
        .constructors
        .filter { !it.hasExportForCompilerAnnotation }
        .filter { isVisibleInObjC(it, session = analysisSession) }
        .sortedWith(getStableCallableOrder(session = analysisSession))
        .flatMap { constructor ->
            val objCConstructor = buildObjCMethod(constructor)
            listOf(objCConstructor) + if (objCConstructor.name == "init") listOf(buildNewInitConstructor(constructor, session = analysisSession)) else emptyList()
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
                origin = getObjCExportStubOrigin(symbol, session = analysisSession),
                isInstanceMethod = false,
                returnType = ObjCInstanceType,
                selectors = listOf("allocWithZone:"),
                parameters = listOf(ObjCParameter("zone", null, ObjCRawType("struct _NSZone *"), null)),
                attributes = listOf("unavailable")
            )
        )
    }

    if (symbol.isCompanion && symbol.classKind != KaClassKind.OBJECT) {
        result.addIfNotNull(addInitIfNeeded(symbol, result))
    }

    // Hide "unimplemented" super constructors:
    context(analysisSession) { getSuperClassSymbolNotAny(symbol)?.memberScope }?.constructors.orEmpty()
        .filter { isVisibleInObjC(it, session = analysisSession) }
        .sortedWith(getStableCallableOrder(session = analysisSession))
        .forEach { superConstructor ->
            val translatedSuperConstructor = buildObjCMethod(superConstructor, unavailable = true)
            if (result.none { it.name == translatedSuperConstructor.name }) {
                result.add(translatedSuperConstructor)
            }
            if (result.none { it.name == "new" } && translatedSuperConstructor.name == "init") {
                /**
                 * There should be only one "new" constructor, so if it's already defined in [symbol] we skip the one from super type
                 */
                result.add(buildNewInitSuperConstructor(superConstructor, session = analysisSession))
            }
        }

    return result
}

/**
 * Additional primary constructor which goes always after primary constructor ([ObjCMethod.name] == "init")
 */
context(session: KaSession)
private fun buildNewInitConstructor(constructor: KaFunctionSymbol): ObjCMethod {
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

/**
 * Additional primary super constructor which goes always after primary constructor ([ObjCMethod.name] == "init")
 */
context(session: KaSession)
private fun buildNewInitSuperConstructor(constructor: KaFunctionSymbol): ObjCMethod {
    return ObjCMethod(
        comment = null,
        origin = getObjCExportStubOrigin(constructor),
        isInstanceMethod = false,
        returnType = ObjCInstanceType,
        selectors = listOf("new"),
        parameters = emptyList(),
        attributes = listOf("unavailable")
    )
}

