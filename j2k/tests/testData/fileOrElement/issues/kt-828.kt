class Test {
    fun test() {
        var res = true
        res = res and false
        res = res or false
        res = res xor false
        System.out.println(true and false)
        System.out.println(true or false)
        System.out.println(true xor false)
        System.out.println(!true)

        System.out.println(true && false)
        System.out.println(true || false)
    }
}