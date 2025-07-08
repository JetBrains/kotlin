/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

interface Displayable {
    fun displayString(): String
}

interface Bar {
    override operator fun equals(other: Any?): Boolean
}

interface DisplayableBar : Displayable, Bar

fun foo(bar: DisplayableBar): Boolean {
    return !bar.equals("zzz") && bar.displayString() == "bar"
}