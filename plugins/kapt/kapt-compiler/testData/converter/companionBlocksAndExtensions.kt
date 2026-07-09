// LANGUAGE: +CompanionBlocks +CompanionExtensions
// WITH_STDLIB

// FILE: J.java
public class J {}

// FILE: k.kt
class A {
    companion {
        val compBlockVal: String = "compBlockVal"
        @JvmOverloads
        fun compBlockFun(k: String = "") = "compBlockFun: " + k
    }
}

companion val A.compExtVal: String = "compExtVal"
@JvmOverloads
companion fun A.compExtFun(k: String = "") = "compExtFun: " + k

companion val J.compExtValJ: String = "compExtValJ"
@JvmOverloads
companion fun J.compExtFunJ(k: String = "") = "compExtFunJ: " + k
