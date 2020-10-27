package library

import kotlin.test.*

val output = mutableListOf<String>()

interface I1 {
    @Test
    fun foo()
}

interface I2 {
    @Test
    fun foo()
}

open class A {

    @BeforeTest
    open fun before() {
        output.add("A.before")
    }

    @AfterTest
    open fun after() {
        output.add("A.after")
    }

    @Test
    open fun test0() {
        output.add("A.test0")
    }

    @Test
    open fun test1() {
        output.add("A.test1")
    }

    @Test
    open fun test2() {
        output.add("A.test2")
    }

    @Test
    open fun test3() {
        output.add("A.test3")
    }

    @Ignore
    @Test
    open fun ignored0() {
        output.add("A.ignored0")
    }

    @Ignore
    @Test
    open fun ignored1() {
        output.add("A.ignored1")
    }

    @Ignore
    @Test
    open fun ignored2() {
        output.add("A.ignored2")
    }
}