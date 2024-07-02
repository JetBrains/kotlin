/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.*

/**
 * See K1 [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportMapperKt.isObjCProperty]
 */
context(KaSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
internal val KaPropertySymbol.isObjCProperty: Boolean
    get() {
        return this.receiverParameter?.type == null || getClassIfCategory() != null
    }
