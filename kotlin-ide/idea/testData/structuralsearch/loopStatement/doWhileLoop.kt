fun a() {
    <warning descr="SSR">do {
        println(0)
    } while(true)</warning>
}

fun b() {
     do {
        println(0)
    } while(false)
}

fun c() {
    do {
        println(1)
    } while(true)
}

fun d() {
    <warning descr="SSR">do println(0) while(true)</warning>
}