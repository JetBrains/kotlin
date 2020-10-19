fun a() {
    <warning descr="SSR">when(10) {
        in 3..10 -> println("In range")
        else ->  println("Not in Range.")
    }</warning>
}