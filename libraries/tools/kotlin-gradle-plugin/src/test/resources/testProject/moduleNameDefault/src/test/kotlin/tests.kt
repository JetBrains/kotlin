package demo 

import org.testng.Assert.assertTrue
import org.testng.annotations.Test as test

class TestSource() {
    @test fun f() {
        assertTrue(isModuleFileExists(), "Kotlin module file should exists")
    }
}

