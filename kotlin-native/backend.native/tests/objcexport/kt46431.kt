/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kt43599

// Based on https://youtrack.jetbrains.com/issue/KT-46431

interface Host {
    val test: String
}
abstract class AbstractHost : Host

fun createAbstractHost(): Host {
    return object : AbstractHost() {
        override val test: String
            get() = "1234"
    }
}
