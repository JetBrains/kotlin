/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.test.tests

import kotlin.js.Promise
import kotlin.test.*

private external fun setTimeout(body: () -> Unit, timeout: Int)

class AsyncTest {

    var log = ""

    var afterLog = ""

    var expectedAfterLog = ""

    @BeforeTest
    fun before() {
        log = ""
        afterLog = ""
        expectedAfterLog = ""
    }

    @AfterTest
    fun after() {
        assertEquals(afterLog, expectedAfterLog)
    }

    fun promise(v: Int, after: String = "") = Promise { resolve, _ ->
        log += "a"
        setTimeout({ log += "c"; afterLog += after; resolve(v.toJsNumber()) }, 100)
        log += "b"
    }.also {
        expectedAfterLog += after
    }

    @Test
    @Ignore //Remove after bootstrap KT-65322
    fun checkAsyncOrder(): Promise<JsNumber> {
        log += 1

        val p1 = promise(10, "after")

        log += 2

        val p2 = p1.then { result ->
            assertEquals(log, "1ab23c")
            result
        }

        log += 3

        return p2
    }

    @Test
    @Ignore //Remove after bootstrap KT-65322
    fun checkCustomPromise(): CustomPromise {
        return promise(10, "") as CustomPromise
    }

    @Test
    @Ignore //Remove after bootstrap KT-65322
    fun asyncPassing() = promise(10).then { assertEquals(10, it.toInt()); it }
}

@JsName("Promise")
external class CustomPromise {}