/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.mergedtree

import org.jetbrains.kotlin.commonizer.cir.CirEntityId

@JvmInline
internal value class CirCommonClassifierId(val aliases: LinkedHashSet<CirEntityId>) {
    override fun toString(): String {
        return aliases.joinToString(prefix = "(", postfix = ")")
    }

    init {
        require(aliases.isNotEmpty())
    }
}