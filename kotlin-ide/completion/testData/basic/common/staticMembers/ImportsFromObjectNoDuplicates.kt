package a

import a.O.xxxVal1
import a.O.xxxFun1

object O {
    fun xxxFun1() { }
    fun xxxFun2() { }

    val xxxVal1 = 1
    val xxxVal2 = 2
}

fun main() {
    xxx<caret>
}

// INVOCATION_COUNT: 2
// EXIST: { allLookupStrings: "xxxFun1", itemText: "xxxFun1" }
// EXIST: { allLookupStrings: "xxxFun2", itemText: "O.xxxFun2" }
// EXIST: { allLookupStrings: "getXxxVal1, xxxVal1", itemText: "xxxVal1" }
// EXIST: { allLookupStrings: "getXxxVal2, xxxVal2", itemText: "O.xxxVal2" }
// NOTHING_ELSE
