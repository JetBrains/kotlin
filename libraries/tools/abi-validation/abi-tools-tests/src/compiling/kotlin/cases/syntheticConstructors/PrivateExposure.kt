/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package cases.syntheticConstructors

class ExposePrivate private constructor(val text: String) {
    class Builder {
        fun build() = ExposePrivate("42")
    }
}

class ExposePrivateMixed constructor(val text: String) {

    private constructor(int: Int) : this(int.toString())

    class Builder {
        fun build() = ExposePrivateMixed(42)
    }
}

class ExposeInternal internal constructor(val text: String) {
    class Builder {
        fun build() = ExposeInternal("42")
    }
}

