// EXTRACTION_TARGET: property with initializer
// WITH_RUNTIME

import java.util.*

class Foo<T> {
    val map = HashMap<String, T>()

    fun test(): T {
        return <selection>map[""]</selection>
    }
}