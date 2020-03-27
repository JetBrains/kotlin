/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.startup

actual class Random actual constructor() {
    @kotlin.native.ThreadLocal
    actual companion object {
        actual var seedInt = 0
        actual fun nextInt(boundary: Int): Int {
            seedInt = (3 * seedInt + 11) % boundary
            return seedInt
        }
    }
}
