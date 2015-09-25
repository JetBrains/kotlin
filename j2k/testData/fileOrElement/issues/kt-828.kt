internal class Test {
    fun test() {
        var res = true
        res = res and false
        res = res or false
        res = res xor false
        println(true and false)
        println(true or false)
        println(true xor false)
        println(!true)

        println(true && false)
        println(true || false)
    }
}