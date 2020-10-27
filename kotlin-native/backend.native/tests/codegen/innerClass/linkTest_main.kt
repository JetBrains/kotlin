/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

open class B : A() {
    open inner class Inner : A.Inner()
}

fun main(args: Array<String>) {
    B().Inner()
}