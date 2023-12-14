/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

class User(val map: Map<String, Any?>) {
    val name: String by map
    val age: Int     by map
}

fun box(): String {
    val user = User(mapOf(
            "name" to "John Doe",
            "age"  to 25
    ))
    assertEquals("John Doe", user.name)
    assertEquals(25, user.age)

    return "OK"
}