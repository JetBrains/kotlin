/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

internal class IrLinkageError(message: String?) : Error(message)

internal fun throwLinkageError(message: String?): Nothing {
    throw IrLinkageError(message)
}