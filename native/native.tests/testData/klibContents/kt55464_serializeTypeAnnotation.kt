package test
class C

// If serializeTypeAnnotation() is wrong, the following callback would have wrong type: (C) -> Unit
fun C.builder(c: C.() -> Unit) {}
