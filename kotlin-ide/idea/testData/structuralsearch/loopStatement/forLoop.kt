fun a() {
    <warning descr="SSR">for(i in 0..10) {
        println(i)
    }</warning>

    for(i in 0..9) {
        println(i)
    }

    for(j in 0..10) {
        println(j)
    }

    <warning descr="SSR">for(i in 0..10) println(i)</warning>
}