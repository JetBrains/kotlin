package tests

fun main(args: Array<String>) {
    print("Varints test...   ")
    VarintsTest.runTests()
    println ("OK!")

    print("Zigzag test...   ")
    ZigZagTest.runTests()
    println("OK!")

    print("Repeated VarInts test...   ")
    RepeatedVarintsTest.runTests()
    println("OK!")
}