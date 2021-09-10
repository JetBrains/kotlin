/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.mergedtree

import org.jetbrains.kotlin.commonizer.cir.CirEntityId

class CirCommonClassifierId(val aliases: List<CirEntityId>) {

    private val _hashCode = aliases.hashCode()

    override fun hashCode(): Int = _hashCode

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is CirCommonClassifierId) return false
        if (other._hashCode != this._hashCode) return false
        if (other.aliases != this.aliases) return false
        return true
    }

    override fun toString(): String {
        return aliases.joinToString(prefix = "(", postfix = ")")
    }

    init {
        require(aliases.isNotEmpty())
    }
}