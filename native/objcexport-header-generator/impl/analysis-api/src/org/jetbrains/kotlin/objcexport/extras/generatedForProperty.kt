/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.extras

import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportStub
import org.jetbrains.kotlin.tooling.core.MutableExtras
import org.jetbrains.kotlin.tooling.core.extrasKeyOf

private val generatedForPropertyKey = extrasKeyOf<Boolean>()

internal var MutableExtras.generatedForProperty: Boolean
    get() = this[generatedForPropertyKey] ?: false
    set(value) {
        this[generatedForPropertyKey] = value
    }

internal val ObjCExportStub.generatedForProperty: Boolean
    get() = extras[generatedForPropertyKey] ?: false
