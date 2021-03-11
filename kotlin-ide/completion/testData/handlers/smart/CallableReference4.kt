class C

fun C.extFun(s: String){}

fun foo(p: C.(String) -> Unit, c: Char){}
fun foo(p: (String) -> Unit){}

fun bar() {
    foo(C::<caret>)
}

// ELEMENT: extFun
