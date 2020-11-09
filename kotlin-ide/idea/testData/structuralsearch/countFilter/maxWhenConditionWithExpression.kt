fun foo(): Int {

    fun f() {}

    <warning descr="SSR">when (1) {
         in 1..10 -> f()
         in 11..20 -> f()
     }</warning>

    val x1 = when { else -> 1 }

    val x2 = <warning descr="SSR">when {
        1 < 2 -> 3
        else -> 1
   }</warning>

    val x3 = when {
        1 < 3 -> 1
        2 > 1 -> 4
        else -> 1
    }

    return x1 + x2 + x3
}