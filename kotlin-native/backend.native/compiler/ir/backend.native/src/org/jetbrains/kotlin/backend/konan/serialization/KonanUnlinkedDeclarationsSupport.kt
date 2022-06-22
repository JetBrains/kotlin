/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.serialization.unlinked.BasicUnlinkedDeclarationsSupport
import org.jetbrains.kotlin.backend.common.serialization.unlinked.UnlinkedDeclarationsSupport.UnlinkedMarkerTypeHandler
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.types.IrErrorType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl

/**
 * Kotlin/Native backend does not support [IrErrorType].
 * So, let's use a special instance of nullable [kotlin.Any] as a marker-type instead.
 */
class KonanUnlinkedDeclarationsSupport(
        override val builtIns: IrBuiltIns,
        override val allowUnboundSymbols: Boolean
) : BasicUnlinkedDeclarationsSupport() {
    override val handler = object : UnlinkedMarkerTypeHandler {
        override val unlinkedMarkerType = IrSimpleTypeImpl(
                classifier = builtIns.anyClass,
                hasQuestionMark = true,
                arguments = emptyList(),
                annotations = emptyList()
        )

        override fun IrType.isUnlinkedMarkerType(): Boolean = this === unlinkedMarkerType
    }
}