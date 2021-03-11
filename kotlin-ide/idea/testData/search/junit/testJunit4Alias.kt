import org.junit.Test as test
import org.junit.Test

class MyTestClass {
    @test fun test1() {}

    @Deprecated @test fun test2() {}

    @Test fun test3() {}
}

// ANNOTATION: org.junit.Test
// SEARCH: method:test1
// SEARCH: method:test2
// SEARCH: method:test3