package sample

import kotlin.Pair
import kotlin.browser.document
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
