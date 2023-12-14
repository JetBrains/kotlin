/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

import kotlin.properties.Delegates

val sb = StringBuilder()

class User {
    var name: String by Delegates.observable("<no name>") {
        prop, old, new ->
        sb.appendLine("$old -> $new")
    }
}

fun box(): String {
    val user = User()
    user.name = "first"
    user.name = "second"

    assertEquals("""
        <no name> -> first
        first -> second

    """.trimIndent(), sb.toString())
    return "OK"
}