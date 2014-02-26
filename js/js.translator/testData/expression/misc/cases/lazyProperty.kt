fun main(args: Array<String>) {
    println("Hello, world!")
    println(p)
}


val p: Int
    get() {
        println("Gotcha")
        return 3
    }