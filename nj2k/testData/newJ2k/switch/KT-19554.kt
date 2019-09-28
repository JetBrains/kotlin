class TestContinueInSwitchInFor {
    private var a = 0
    private var b = 0
    private var c = 0

    fun foo(cc: CharArray) {
        var i = 0
        loop@ while (i < cc.size && cc[i] != ';') {
            when (cc[i]) {
                ' ' -> {
                    ++i
                    continue@loop
                }
                'a' -> a++
                'b' -> b++
                'c' -> c++
            }
            ++i
        }
        println("a = $a; b = $b; c = $c")
    }

    fun fooWithLabel(cc: CharArray) {
        var i = 0
        Loop@ while (i < cc.size && cc[i] != ';') {
            when (cc[i]) {
                ' ' -> {
                    ++i
                    continue@Loop
                }
                'a' -> a++
                'b' -> b++
                'c' -> c++
            }
            ++i
        }
        println("a = $a; b = $b; c = $c")
    }
}
