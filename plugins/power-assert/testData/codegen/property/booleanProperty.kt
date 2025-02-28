// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters

fun box(): String {
    return test1() +
            Member().test2() +
            test3() +
            test4() +
            test5() +
            test6() +
            test7() +
            test8() +
            test9()
}

fun test1() = expectThrowableMessage {
    val booleanValue = false
    assert(booleanValue)
}

class Member {
    var memberProperty: Boolean
        get() = false
        set(value) { }

    fun test2() = expectThrowableMessage {
        assert(memberProperty)
    }
}

fun test3() = expectThrowableMessage {
    val delegatedProperty: Boolean by lazy { false }
    assert(delegatedProperty)
}

val Int.extensionTopLevelProperty: Boolean
    get() = false

fun test4() = expectThrowableMessage {
    assert(1.extensionTopLevelProperty)
}

val <T> T.extensionTopLevelPropertyWithTypeParam: T
    get() = false as T

fun test5() = expectThrowableMessage {
    assert(true.extensionTopLevelPropertyWithTypeParam)
}

context(a: Boolean)
val contextProperty: Boolean
    get() = a

fun test6() = expectThrowableMessage {
    assert(with(false) { contextProperty })
}

context(a: T)
val <T> T.contextPropertyWithTypeParam: T
    get() = a

fun test7() = expectThrowableMessage {
    with(false) {
        assert(true.contextPropertyWithTypeParam)
    }
}

lateinit var lateInitProperty: String

fun test8() = expectThrowableMessage {
    lateInitProperty = "a"
    assert(lateInitProperty == "b")
}

fun test9() = expectThrowableMessage {
    val (destructuring, declaration) = Pair(false, false)
    assert(destructuring)
}


