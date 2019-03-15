// EXPECTED_REACHABLE_NODES: 1288
/*
Modified test case from issue: https://youtrack.jetbrains.com/issue/KT-24542
 */
package foo

class Test() {
    var output: String = ""

    inline fun foo(): Boolean {
        output += "foo "
        return false
    }

    fun run(doBreak: Boolean, doContinue: Boolean) {
        do {
            output += "1 "
            if (doBreak) break
            output += "2 "
            if (doContinue) continue
            output += "3 "
        }
        while(foo())
    }

    fun runNested(doBreak: Boolean, doContinue: Boolean) {
        do {
            output += "0_1 "

            do {
                output += "1_1 "
                if (doBreak) break
                output += "1_2 "
                if (doContinue) continue
                output += "1_3 "
            }
            while(foo())

            output += "0_2 "

            if (doBreak) break

            output += "0_3 "

            do {
                output += "2_1 "
                if (doBreak) break
                output += "2_2 "
                if (doContinue) continue
                output += "2_3 "
            }
            while(foo())

            output += "0_4 "

            if (doContinue) continue

            output += "0_5 "

            loop_with_label@ do {
                output += "3_1 "
                if (doBreak) break
                output += "3_2 "
                if (doContinue) continue
                output += "3_3 "
            }
            while(foo())

            output += "0_6 "
        }
        while(foo())
    }
}

fun test(doBreak: Boolean, doContinue: Boolean): String {
    var x = Test()
    x.run(doBreak, doContinue)
    return x.output
}

fun testNested(doBreak: Boolean, doContinue: Boolean): String {
    var x = Test()
    x.runNested(doBreak, doContinue)
    return x.output
}

fun box(): String {
    val test1 = test(true, true)
    val test2 = test(true, false)
    val test3 = test(false, true)
    val test4 = test(false, false)

    if (test1 != "1 ") return "Test1 output: ${test1}"
    if (test2 != "1 ") return "Test2 output: ${test2}"
    if (test3 != "1 2 foo ") return "Test3 output: ${test3}"
    if (test4 != "1 2 3 foo ") return "Test4 output: ${test4}"

    val testNested1 = testNested(true, true)
    val testNested2 = testNested(true, false)
    val testNested3 = testNested(false, true)
    val testNested4 = testNested(false, false)

    if (testNested1 != "0_1 1_1 0_2 ") return "testNested1 output: ${testNested1}"
    if (testNested2 != "0_1 1_1 0_2 ") return "testNested2 output: ${testNested2}"
    if (testNested3 != "0_1 1_1 1_2 foo 0_2 0_3 2_1 2_2 foo 0_4 foo ") return "testNested3 output: ${testNested3}"
    if (testNested4 != "0_1 1_1 1_2 1_3 foo 0_2 0_3 2_1 2_2 2_3 foo 0_4 0_5 3_1 3_2 3_3 foo 0_6 foo ")
        return "testNested4 output: ${testNested4}"

    return "OK"
}