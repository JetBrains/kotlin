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

    print("Repeated ZigZags test...   ")
    RepeatedZigZagTest.runTests()
    println("OK!")

    print("Tag order test...   ")
    TagOrderTest.runTests()
    println("OK!")

    print("Cross-branch access test...   ")
    CrossBranchTest.runTests()
    println("OK!")

    print("Location.proto test...   ")
    LocationTest.runTests()
    println("OK!")

    print("Connect.proto test...   ")
    ConnectTest.runTests()
    println("OK!")

    print("Base & Extended test...   ")
    BaseExtendedTest.runTests()
    println("OK!")
}