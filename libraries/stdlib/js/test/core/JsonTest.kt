/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

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