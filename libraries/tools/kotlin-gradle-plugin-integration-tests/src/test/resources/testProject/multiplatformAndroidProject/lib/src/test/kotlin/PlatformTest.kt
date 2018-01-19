package foo

expect class PlatformTest {
    val value: PlatformClass
}

class CommonTest(val commonClass: CommonClass)