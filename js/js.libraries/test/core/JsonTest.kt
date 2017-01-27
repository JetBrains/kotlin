package test.js

import kotlin.js.*
import kotlin.test.*
import org.junit.Test as test

class JsonTest {

    @test fun createJsonFromPairs() {
        var obj = json(Pair("firstName", "John"), Pair("lastName", "Doe"), Pair("age", 30))
        assertEquals("John", obj["firstName"], "firstName")
        assertEquals("Doe", obj["lastName"], "lastName")
        assertEquals(30, obj["age"], "age")
    }
}