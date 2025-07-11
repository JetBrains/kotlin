/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

// TODO(KT-78243): Delete this file after 2.2.20 branching

@Deprecated("Use kotlin.internal.IrLinkageError instead", level = DeprecationLevel.HIDDEN)
internal class IrLinkageError(message: String?) : Error(message)

@Deprecated("Use kotlin.internal.throwIrLinkageError instead", level = DeprecationLevel.HIDDEN)
internal fun throwLinkageError(message: String?): Nothing {
    kotlin.internal.throwIrLinkageError(message)
}
