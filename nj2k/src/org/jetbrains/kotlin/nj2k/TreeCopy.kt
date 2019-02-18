/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k

import org.jetbrains.kotlin.nj2k.tree.JKElement
import org.jetbrains.kotlin.nj2k.tree.impl.JKElementBase
import org.jetbrains.kotlin.nj2k.tree.withNonCodeElementsFrom

fun <T : JKElement> T.copyTree(): T =
    when (this) {
        is JKElementBase ->
            this.copy().withNonCodeElementsFrom(this) as T
        else -> TODO("Not supported+$this.toString()")
    }

fun <T : JKElement> T.copyTreeAndDetach(): T =
    this.copyTree().also {
        if (it.parent != null) it.detach(it.parent!!)
    }

