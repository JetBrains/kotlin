package demo

import org.testng.Assert.*
import org.testng.annotations.Test as test

class TestSource() {
    @test fun f() {
        assertEquals(box(), "OK")
    }
}

