package org.jetbrains

enum class Sign(val element: String) {
    AARD("telekinetic"),
    IGNI("fire"),
    QUEN("protective"),
    YRDEN("trap"),
    AXII("mind control");
}

class Witcher(val name: String, val favoriteSign: Sign) {
    fun cast(): String = "$name casts ${favoriteSign.name}, a ${favoriteSign.element} sign"
}
