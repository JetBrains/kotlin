class A {
    var i = 1

    fun foo() {
        run {
            var i = 1
            while (i < 1000) {
                System.out.println(i)
                i *= 2
            }
        }

        i = 10
    }
}
