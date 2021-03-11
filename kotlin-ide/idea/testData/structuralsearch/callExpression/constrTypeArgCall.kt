class A<T, R>(val b: T, val c: R)

fun d(): A<Int, String> {
    return <warning descr="SSR">A<Int, String>(0, "a")</warning>
}