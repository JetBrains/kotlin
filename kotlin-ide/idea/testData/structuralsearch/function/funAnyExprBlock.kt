fun f(): String = <warning descr="SSR">"s"</warning>
fun f2(): String {
    return <warning descr="SSR">""</warning>
}
fun f3(): String {
    println()
    return ""
}