/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.mergedtree

import org.jetbrains.kotlin.commonizer.cir.CirClass

internal class CirMemberContext private constructor(val classes: List<CirClass>) {

    companion object {
        val empty = CirMemberContext(emptyList())
    }

    fun withContextOf(clazz: CirClass) = CirMemberContext(classes = classes + clazz)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CirMemberContext
        if (classes != other.classes) return false
        return true
    }

    override fun hashCode(): Int {
        return classes.hashCode()
    }
}