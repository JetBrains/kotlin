package com.example.bar

actual value class Bar actual constructor(actual val value: String){
    override fun toString(): String {
        return value
    }
}

actual value class Foo actual constructor(actual val value: String){
    actual override fun toString(): String {
        return value
    }
}
