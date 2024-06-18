/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCClass
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportStub
import org.jetbrains.kotlin.utils.addIfNotNull

context(KaSession, KtObjCExportSession)
internal fun KaCallableSymbol.translateToObjCExportStub(): List<ObjCExportStub> {
    val result = mutableListOf<ObjCExportStub>()
    when (this) {
        is KaPropertySymbol -> {
            if (isObjCProperty) {
                result.addIfNotNull(translateToObjCProperty())
            } else {
                result.addIfNotNull(this.getter?.translateToObjCMethod())
                result.addIfNotNull(this.setter?.translateToObjCMethod())
            }
        }
        is KaNamedFunctionSymbol -> result.addIfNotNull(translateToObjCMethod())
        else -> Unit
    }
    return result
}

context(KaSession, KtObjCExportSession)
internal fun KaClassSymbol.translateToObjCExportStub(): ObjCClass? = when (classKind) {
    KaClassKind.INTERFACE -> translateToObjCProtocol()
    KaClassKind.CLASS -> translateToObjCClass()
    KaClassKind.OBJECT -> translateToObjCObject()
    KaClassKind.ENUM_CLASS -> translateToObjCClass()
    KaClassKind.COMPANION_OBJECT -> translateToObjCObject()
    else -> null
}