// LANGUAGE: +NestedTypeAliases
// ISSUE: KT-79185

// SNIPPET

class C(val p: String)

typealias LocalTAThatLooksTopLevel = C

val obj: <!UNRESOLVED_REFERENCE!>LocalTAThatLooksTopLevel<!> = <!UNRESOLVED_REFERENCE!>LocalTAThatLooksTopLevel<!>("OK")
val rv1 = obj.<!UNRESOLVED_REFERENCE!>p<!>

// EXPECTED: rv1 == "OK"

