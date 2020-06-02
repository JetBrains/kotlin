fun a() {
    <warning descr="SSR">while(true) {
        println(0)
    }</warning>
}

fun b() {
    while(false) {
        println(0)
    }
}

fun c() {
    while(true) {
        println(1)
    }
}

fun d() {
    <warning descr="SSR">while(true) println(0)</warning>
}