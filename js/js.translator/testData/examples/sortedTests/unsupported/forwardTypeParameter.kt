class Foo() {
}
class Bar() {
}

fun <T> isInstance(obj: Any?) = obj is T

fun <T> isInstance2(obj: Any?) = isInstance<T>(obj)

fun box(): String {
    if (!isInstance2<Foo>(Foo())) return "fail 1"
    if (isInstance2<Bar>(Foo())) return "fail 2"
    return "OK"
}
