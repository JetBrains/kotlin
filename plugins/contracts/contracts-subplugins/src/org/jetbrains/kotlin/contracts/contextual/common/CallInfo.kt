/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual.common

import org.jetbrains.kotlin.contracts.description.InvocationKind
import org.jetbrains.kotlin.psi.KtElement

internal data class CallInfo(val sourceElement: KtElement, val kind: InvocationKind) {
    override fun toString(): String = kind.toString()
}