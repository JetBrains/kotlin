package kt_java_tests

import java_msg.Varints

/**
 * Created by user on 8/11/16.
 */

fun main(args: Array<String>) {
    print("Base-Extended tests...")
    BaseExtendedTest.runTests()
    println("  OK!")

    print("Connect Tests...")
    ConnectTest.runTests()
    println("  OK!")

    print("Cross-branch access tests...")
    CrossBranchTest.runTests()
    println("  OK!")

    print("Direction Tests...")
    DirectionTest.runTests()
    println("  OK!")

    print("Location Tests...")
    Location.runTests()
    println("  OK!")

    print("Multiple Messages Tests...")
    MultipleMessagesTest.runTests()
    println("  OK!")

    print("Repeated VarInts Tests...")
    RepeatedVarintsTest.runTests()
    println("  OK!")

    print("Repeated ZigZag Tests...")
    RepeatedZigZag.runTests()
    println("  OK!")

    print("Tag order Tests...")
    TagOrderTests.runTests()
    println("  OK!")

    print("Varints Tests...")
    VarintsTest.runTests()
    println("  OK!")

    print("ZigZag Tests...")
    ZigZagTests.runTests()
    println("  OK!")
}