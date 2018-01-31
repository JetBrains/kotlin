package test.js

import kotlin.js.*
import kotlin.test.*

class JsonTest {

    @Test fun createJsonFromPairs() {
        var obj = json(Pair("firstName", "John"), Pair("lastName", "Doe"), Pair("age", 30))
        assertEquals("John", obj["firstName"], "firstName")
        assertEquals("Doe", obj["lastName"], "lastName")
        assertEquals(30, obj["age"], "age")
    }
}