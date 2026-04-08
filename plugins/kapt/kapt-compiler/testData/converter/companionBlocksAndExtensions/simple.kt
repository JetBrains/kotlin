// LANGUAGE: +CompanionBlocksAndExtensions
// WITH_SDLIB

// FILE: J.java
public class J {}

// FILE: k.kt
class A {

    companion {
        val compBlockVal: String = "compBlockVal"
        fun compBlockFun(k: String = "") {println("compBlockFun: " + k)}
    }


}
companion val A.compExtVal: String = "compExtVal"
companion fun A.compExtFun(k: String = "") {println("compExtFun: " + k)}

companion val J.compExtValJ: String = "compExtValJ"
companion fun J.compExtFunJ(k: String = "") {println("compExtFunJ: " + k)}
