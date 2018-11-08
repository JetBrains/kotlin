/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual.exceptions

import org.jetbrains.kotlin.contracts.contextual.model.ContextFamily

internal object ExceptionFamily : ContextFamily {
    override val id: String = "Checked exceptions"
    override val combiner = ExceptionContextCombiner
    override val emptyContext = ExceptionContext()

    override fun toString(): String = id
}