/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package hair.opt

import hair.ir.*
import hair.utils.whileChanged

fun Session.optimize() {
    whileChanged(100) {
        fun Boolean.trackChange() { if (this) changed() }

        simplify().trackChange()
        eliminateDead().trackChange()
    }
}