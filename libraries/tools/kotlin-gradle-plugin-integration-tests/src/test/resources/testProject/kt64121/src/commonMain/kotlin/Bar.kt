package com.example.bar

expect value class Bar(val value: String)

expect value class Foo(val value: String) {
    override fun toString(): String
}
