/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.descriptors.FunctionDescriptor

interface ObjCExportProblemCollector {
    fun reportWarning(text: String)
    fun reportWarning(method: FunctionDescriptor, text: String)
    fun reportException(throwable: Throwable)

    object SILENT : ObjCExportProblemCollector {
        override fun reportWarning(text: String) {}
        override fun reportWarning(method: FunctionDescriptor, text: String) {}
        override fun reportException(throwable: Throwable) {}
    }
}