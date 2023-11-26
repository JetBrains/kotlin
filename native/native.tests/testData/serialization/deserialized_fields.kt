/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
import kotlin.test.*

class CustomThrowable(val extraInfo: String): Throwable("Some message", null)

fun box(): String {
    val custom = CustomThrowable("Extra info")
    assertEquals(custom.extraInfo, "Extra info")
    assertEquals(custom.message, "Some message")
    assertEquals(custom.cause, null)

    return "OK"
}