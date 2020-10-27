/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

sealed class Tag {
    abstract fun value(): Any
}

sealed class TagBoolean : Tag() {
    abstract override fun value(): Boolean

    object True : TagBoolean() {
        override fun value() = true
    }

    object False : TagBoolean() {
        override fun value() = false
    }
}