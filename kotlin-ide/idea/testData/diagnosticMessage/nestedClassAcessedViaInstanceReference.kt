// !DIAGNOSTICS_NUMBER: 4
// !DIAGNOSTICS: NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE

class A {
    class Nested()
    class NestedWithClassObject { companion object }
    enum class NestedEnum { A }
    object NestedObj { fun invoke() = 1 }
    typealias NestedAlias = Nested
}

fun test(a: A) {
    a.NestedWithClassObject
    a.NestedEnum
    a.NestedObj
    a.NestedAlias
    a.Nested() // TODO: report "nested class accessed via instance reference" here as well
}
