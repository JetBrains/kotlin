// ENABLE_SERIALIZATION
// ENABLE_PARCELIZE
// WITH_STDLIB
// ISSUE: KT-68162

interface A
interface B : A

fun box(): String = "OK"
