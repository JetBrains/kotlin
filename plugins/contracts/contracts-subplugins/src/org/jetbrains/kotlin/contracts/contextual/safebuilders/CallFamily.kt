/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual.safebuilders

import org.jetbrains.kotlin.contracts.contextual.model.ContextFamily

internal object CallFamily : ContextFamily {
    override val id = "Safe builders"
    override val combiner = CallCombiner
    override val emptyContext = CallContext()

    override fun toString(): String {
        return id
    }
}