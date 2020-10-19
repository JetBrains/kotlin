fun foo() {
    print(<warning descr="SSR">when (1) {
        in 0..3 -> 2
        else -> 3
    }</warning>)
    print(<warning descr="SSR">when { else -> true }</warning>)
}

