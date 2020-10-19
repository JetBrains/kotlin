fun a() {
    <warning descr="SSR">when(val b = false) {
        true -> println(b)
        false ->  println(b)
    }</warning>
}