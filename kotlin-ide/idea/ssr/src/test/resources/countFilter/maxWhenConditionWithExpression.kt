fun foo() {

    print(when { else -> 1 })

    print(<warning descr="SSR">when {
        1 < 2 -> 3
        else -> 1 }</warning>)

    print(when {
        1 < 3 -> 1
        2 > 1 -> 4
        else -> 1 })

}