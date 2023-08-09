/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.irText

import org.jetbrains.kotlin.backend.konan.KonanFqNames
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.util.DumpIrTreeOptions
import org.jetbrains.kotlin.test.backend.handlers.IrTextDumpHandler
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.model.BackendKind
import org.jetbrains.kotlin.test.services.TestServices

class NativeIrTextDumpHandler(
    testServices: TestServices,
    artifactKind: BackendKind<IrBackendInput>,
) : IrTextDumpHandler(
    testServices,
    artifactKind
) {
    override val dumpOptions = super.dumpOptions.copy(
        annotationFilter = {
            it.type.classFqName != KonanFqNames.gcUnsafeCall
        },
        doPrintExternalFlag = { // KT-61141
            it.isExternal  && !(it.isFakeOverride && it.name.asString() == "equals")
        },
    )
}