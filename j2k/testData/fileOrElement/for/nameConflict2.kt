internal class A {
    fun foo() {
        run {
            var i = 1
            var j = 0
            while (i < 1000) {
                println(i)
                i *= 2
                j++
            }
        }

        var j = 1
        while (j < 2000) {
            println(j)
            j *= 2
        }
    }
}
