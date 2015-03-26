class A {
    fun foo() {
        run {
            var i = 1
            while (i < 1000) {
                System.out.println(i)
                i *= 2
            }
        }

        var i = 1
        while (i < 2000) {
            System.out.println(i)
            i *= 2
        }
    }
}
