/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.backend.konan.objcexport.ObjCTopLevel
import org.jetbrains.kotlin.backend.konan.objcexport.objCBaseDeclarations
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getDefaultSuperClassOrProtocolName

fun KtObjCExportSession.objCBaseDeclarations(): List<ObjCTopLevel> {
    return objCBaseDeclarations(
        topLevelNamePrefix = configuration.frameworkName.orEmpty(),
        objCNameOfAny = getDefaultSuperClassOrProtocolName(),
        objCNameOfNumber = getObjCKotlinStdlibClassOrProtocolName("Number"),
        objCNameOfMutableMap = getObjCKotlinStdlibClassOrProtocolName("MutableDictionary"),
        objCNameOfMutableSet = getObjCKotlinStdlibClassOrProtocolName("MutableSet"),
        objCNameForNumberBox = { classId -> getObjCKotlinStdlibClassOrProtocolName(classId.shortClassName.asString()) }
    )
}
