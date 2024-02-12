/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.backend.konan.objcexport.ObjCTopLevel
import org.jetbrains.kotlin.backend.konan.objcexport.objCBaseDeclarations
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getDefaultSuperClassOrProtocolName

context(KtObjCExportSession)
internal fun objCBaseDeclarations(): List<ObjCTopLevel> {
    return objCBaseDeclarations(
        topLevelNamePrefix = configuration.frameworkName.orEmpty(),
        objCNameOfAny = getDefaultSuperClassOrProtocolName(),
        objCNameOfNumber = "Number".getObjCKotlinStdlibClassOrProtocolName(),
        objCNameOfMutableMap = "MutableDictionary".getObjCKotlinStdlibClassOrProtocolName(),
        objCNameOfMutableSet = "MutableSet".getObjCKotlinStdlibClassOrProtocolName(),
        objCNameForNumberBox = { classId -> classId.shortClassName.asString().getObjCKotlinStdlibClassOrProtocolName() }
    )
}
