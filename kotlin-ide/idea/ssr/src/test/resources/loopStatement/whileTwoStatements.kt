fun foo() {
    var x = true

    while (x)
        println()

    while (x) {
        println()
    }

    <warning descr="SSR">while (x) {
        println()
        println()
    }</warning>

}