import org.junit.Test as unittest
import org.junit.Test

class MyTestClass {
    @unittest fun test1() {}

    @Deprecated @unittest fun test2() {}

    @Test fun test3() {}
}

// ANNOTATION: org.junit.Test
// SEARCH: method:test1
// SEARCH: method:test2
// SEARCH: method:test3