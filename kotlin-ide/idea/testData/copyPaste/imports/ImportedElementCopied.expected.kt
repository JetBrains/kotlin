// ERROR: Unresolved reference: Inner
// ERROR: Unresolved reference: Nested
// ERROR: Unresolved reference: NestedEnum
// ERROR: Unresolved reference: NestedObj
// ERROR: Unresolved reference: NestedTrait
// ERROR: Unresolved reference: NestedAnnotation
package to

class Outer {
    inner class Inner {
    }
    class Nested {
    }
    enum class NestedEnum {
    }
    object NestedObj {
    }
    interface NestedTrait {
    }
    annotation class NestedAnnotation
}

enum class E {
    ENTRY
}

fun f2(i: Inner, n: Nested, e: NestedEnum, o: NestedObj, t: NestedTrait, a: NestedAnnotation) {
}