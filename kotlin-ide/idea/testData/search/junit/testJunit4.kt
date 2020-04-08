import org.junit.Test

class MyTestClass {
    @org.junit.Test fun test1() {}

    @org.junit.Test fun test2() {}

    @Test fun test3() {}

    @Test fun test4() {}

    @Deprecated @org.junit.Test fun test5() {}

    @Deprecated @Test fun test6() {}

    fun test7() {}
}

// ANNOTATION: org.junit.Test
// SEARCH: method:test1
// SEARCH: method:test2
// SEARCH: method:test3
// SEARCH: method:test4
// SEARCH: method:test5
// SEARCH: method:test6