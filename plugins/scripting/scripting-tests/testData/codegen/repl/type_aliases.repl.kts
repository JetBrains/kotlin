// LANGUAGE: +NestedTypeAliases
// ISSUE: KT-79185

// SNIPPET

class C(val p: String)

typealias LocalTAThatLooksTopLevel = C

val obj: LocalTAThatLooksTopLevel = LocalTAThatLooksTopLevel("OK")
val rv1 = obj.p

// EXPECTED: rv1 == "OK"

