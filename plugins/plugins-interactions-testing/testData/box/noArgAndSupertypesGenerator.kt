// ENABLE_SERIALIZATION
// WITH_STDLIB
// ISSUE: KT-80944

open class Base

class Derived : Base()

fun box(): String = "OK"
