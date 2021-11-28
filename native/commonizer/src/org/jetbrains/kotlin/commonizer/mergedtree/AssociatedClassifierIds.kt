/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.mergedtree

import org.jetbrains.kotlin.commonizer.cir.CirEntityId

class AssociatedClassifierIds(val ids: Set<CirEntityId>) {

    private val _hashCode = ids.hashCode()

    override fun hashCode(): Int = _hashCode

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is AssociatedClassifierIds) return false
        if (other._hashCode != this._hashCode) return false
        if (other.ids != this.ids) return false
        return true
    }

    override fun toString(): String {
        return ids.joinToString(prefix = "(", postfix = ")")
    }

    init {
        require(ids.isNotEmpty())
    }
}