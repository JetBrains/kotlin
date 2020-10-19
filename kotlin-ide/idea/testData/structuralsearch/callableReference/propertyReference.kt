val x = 1

fun main() {
    println(::x.get())
    println(<warning descr="SSR">::x.name</warning>)
}