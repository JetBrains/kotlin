fun main(args: Array<String>) {
    println(M1().foo())
    println(M1().bar<M1>().simpleName)
}