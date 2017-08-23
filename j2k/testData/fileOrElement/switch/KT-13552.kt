object SwitchDemo {
    fun test(i: Int): Int {
        var monthString = "<empty>"
        when (i) {
            1 -> {
                print(1)
                print(2)
                print(3)
                print(4)
                print(5)
            }
            2 -> {
                print(2)
                print(3)
                print(4)
                print(5)
            }
            3 -> {
                print(3)
                print(4)
                print(5)
            }
            4 -> {
                print(4)
                print(5)
            }
            5 -> print(5)
            6 -> {
                print(6)
                print(7)
                print(8)
                print(9)
                print(10)
                print(11)
                monthString = "December"
            }
            7 -> {
                print(7)
                print(8)
                print(9)
                print(10)
                print(11)
                monthString = "December"
            }
            8 -> {
                print(8)
                print(9)
                print(10)
                print(11)
                monthString = "December"
            }
            9 -> {
                print(9)
                print(10)
                print(11)
                monthString = "December"
            }
            10 -> {
                print(10)
                print(11)
                monthString = "December"
            }
            11 -> {
                print(11)
                monthString = "December"
            }
            12 -> monthString = "December"
            else -> {
                print(4)
                print(5)
            }
        }
        val status = ""
        when (status) {
            "init", "dial", "transmit" -> return 0x111111
            "ok" -> return -0xff9a00
            "cancel" -> return -0x99999a
            "fail", "busy", "error" -> return -0x9a0000
            else -> return -0x9a0000
        }
    }
}