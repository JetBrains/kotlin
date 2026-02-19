// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters
fun box(): String = runAll(
    "test1" to { test1() },
    "test2" to { Member().test2() },
    "test3" to { test3() },
    "test4" to { test4() },
    "test5" to { test5() },
    "test6" to { test6() },
    "test7" to { test7() },
    "test8" to { test8() },
    "test9" to { test9() },
)

fun test1() {
    val booleanValue = false
    assert(booleanValue)
}

class Member {
    var memberProperty: Boolean
        get() = false
        set(value) { }

    fun test2() {
        assert(memberProperty)
    }
}

fun test3() {
    val delegatedProperty: Boolean by lazy { false }
    assert(delegatedProperty)
}

val Int.extensionTopLevelProperty: Boolean
    get() = false

fun test4() {
    assert(1.extensionTopLevelProperty)
}

val <T> T.extensionTopLevelPropertyWithTypeParam: T
    get() = false as T

fun test5() {
    assert(true.extensionTopLevelPropertyWithTypeParam)
}

context(a: Boolean)
val contextProperty: Boolean
    get() = a

fun test6() {
    assert(with(false) { contextProperty })
}

context(a: T)
val <T> T.contextPropertyWithTypeParam: T
    get() = a

fun test7() {
    with(false) {
        assert(true.contextPropertyWithTypeParam)
    }
}

lateinit var lateInitProperty: String

fun test8() {
    lateInitProperty = "a"
    assert(lateInitProperty == "b")
}

fun test9() {
    val (destructuring, declaration) = Pair(false, false)
    assert(destructuring)
}


