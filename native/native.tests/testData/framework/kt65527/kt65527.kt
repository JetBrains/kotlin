/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

interface IKt65527<T> {
    val prop: T
}

class CKt65527: IKt65527<Boolean> {
    private var _prop = true

    override var prop: Boolean
        get() = _prop
        set(value) {
            println("new value: $value")
            _prop = value
        }
}
