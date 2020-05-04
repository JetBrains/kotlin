fun a() {
    <warning descr="SSR">try {
        println(0)
    } finally {
        println(1)
    }</warning>

    try {
        println(0)
    } finally {
        println(2)
    }

    try {
        println(1)
    } finally {
        println(1)
    }
}