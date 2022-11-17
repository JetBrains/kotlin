/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package sample

import kotlin.Pair
import kotlinx.browser.document
import library.sample.*
import kotlin.js.Date

fun myApp() {
    val element = document.getElementById("foo")
    if (element != null) {
        val p = Pair(10, 20)
        val x = pairAdd(p)
        val y = pairMul(p)
        val z = IntHolder(100).value
        val u = Date().extFun()
        element.appendChild(document.createTextNode("x=$x y=$y z=$z u=$u"))
    }
}
