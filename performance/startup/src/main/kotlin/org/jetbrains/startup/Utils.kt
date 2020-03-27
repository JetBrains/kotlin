/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.startup

expect class Random() {
    companion object {
        var seedInt: Int
        fun nextInt(boundary: Int = 100): Int
    }
}
