fun <T, R> a(b: T, c: R): T? {
    return if(c is String) b else null
}

fun d(): Int? {
    return <warning descr="SSR">a<Int, String>(0, "a")</warning>
}