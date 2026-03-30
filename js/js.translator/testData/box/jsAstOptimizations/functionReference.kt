// FILE: a.kt

// EXPECT_GENERATED_JS: function=topLevel$ref expect=functionReference.topLevel.js
fun topLevel() {}

class Foo {
    // EXPECT_GENERATED_JS: function=Foo$bar$ref expect=functionReference.method.js
    fun bar() {}
}

fun call(f: () -> Unit) { f() }
fun Foo.call(f: Foo.() -> Unit) { f() }

// FILE: b.kt
fun consumer1(): String {
    call(::topLevel)
    if (::topLevel != ::topLevel)
        return "fail: topLevel is not equal to itself"

    Foo().call(Foo::bar)
    if (Foo::bar != Foo::bar)
        return "fail: Foo.bar is not equal to itself"

    return "OK"
}

// FILE: c.kt
fun consumer2(): String {
    call(::topLevel)
    if (::topLevel != ::topLevel)
        return "fail: topLevelCallable is not equal to itself"

    Foo().call(Foo::bar)
    if (Foo::bar != Foo::bar)
        return "fail: Foo.bar is not equal to itself"

    return "OK"
}

// FILE: main.kt
fun box(): String {
    val result1 = consumer1()
    if (result1 != "OK") return result1
    val result2 = consumer2()
    if (result2 != "OK") return result2
    return "OK"
}