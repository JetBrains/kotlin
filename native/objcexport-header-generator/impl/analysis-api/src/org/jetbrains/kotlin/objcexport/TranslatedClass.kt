/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.backend.konan.objcexport.ObjCClass
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCTopLevel

class TranslatedClass(
    val auxiliaryDeclarations: List<ObjCTopLevel>,
    val objCClass: ObjCClass,
)

fun TranslatedClass(objCClass: ObjCClass?) = objCClass?.let { TranslatedClass(emptyList(), it) }

