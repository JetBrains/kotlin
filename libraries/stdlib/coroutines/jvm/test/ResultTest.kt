/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package test.kotlin

import kotlin.test.*

class ResultTest {
    @Test
    fun testRunCatchingSuccess() {
        val ok = runCatching { "OK" }
        checkSuccess(ok, "OK", true)
    }

    @Test
    fun testRunCatchingFailure() {
        val fail = runCatching { error("F") }
        checkFailure(fail, "F", true)
    }

    @Test
    fun testConstructedSuccess() {
        val ok = Result.success("OK")
        checkSuccess(ok, "OK", true)
    }

    @Test
    fun testConstructedFailure() {
        val fail = Result.failure<Unit>(IllegalStateException("F"))
        checkFailure(fail, "F", true)
    }

    private fun <T> checkSuccess(ok: Result<T>, v: T, topLevel: Boolean = false) {
        assertTrue(ok.isSuccess)
        assertFalse(ok.isFailure)
        assertEquals(v, ok.getOrThrow())
        assertEquals(v, ok.getOrElse { throw it })
        assertEquals(v, ok.getOrNull())
        assertEquals(v, ok.getOrElse { null })
        assertEquals(v, ok.getOrDefault("DEF"))
        assertEquals(v, ok.getOrElse { "EX:$it" })
        assertEquals("V:$v", ok.fold({ "V:$it" }, { "EX:$it" }))
        assertEquals(null, ok.exceptionOrNull())
        assertEquals(null, ok.fold(onSuccess = { null }, onFailure = { it }))
        assertEquals("Success($v)", ok.toString())
        assertEquals(ok, ok)
        if (topLevel) {
            checkSuccess(ok.map { 42 }, 42)
            checkSuccess(ok.mapCatching { 42 }, 42)
            checkFailure(ok.mapCatching { error("FAIL") }, "FAIL")
            checkSuccess(ok.recover { 42 }, "OK")
            checkSuccess(ok.recoverCatching { 42 }, "OK")
            checkSuccess(ok.recoverCatching { error("FAIL") }, "OK")
        }
        var sCnt = 0
        var fCnt = 0
        assertEquals(ok, ok.onSuccess { sCnt++ })
        assertEquals(ok, ok.onFailure { fCnt++ })
        assertEquals(1, sCnt)
        assertEquals(0, fCnt)
    }

    private fun <T> checkFailure(fail: Result<T>, msg: String, topLevel: Boolean = false) {
        assertFalse(fail.isSuccess)
        assertTrue(fail.isFailure)
        assertFails { fail.getOrThrow() }
        assertFails { fail.getOrElse { throw it } }
        assertEquals(null, fail.getOrNull())
        assertEquals(null, fail.getOrElse { null })
        assertEquals("DEF", fail.getOrDefault("DEF"))
        assertEquals("EX:java.lang.IllegalStateException: $msg", fail.getOrElse { "EX:$it" })
        assertEquals("EX:java.lang.IllegalStateException: $msg", fail.fold({ "V:$it" }, { "EX:$it" }))
        assertEquals(msg, fail.exceptionOrNull()!!.message)
        assertEquals(msg, fail.fold(onSuccess = { null }, onFailure = { it })!!.message)
        assertEquals("Failure(java.lang.IllegalStateException: $msg)", fail.toString())
        assertEquals(fail, fail)
        if (topLevel) {
            checkFailure(fail.map { 42 }, msg)
            checkFailure(fail.mapCatching { 42 }, msg)
            checkFailure(fail.mapCatching { error("FAIL") }, msg)
            checkSuccess(fail.recover { 42 }, 42)
            checkSuccess(fail.recoverCatching { 42 }, 42)
            checkFailure(fail.recoverCatching { error("FAIL") }, "FAIL")
        }
        var sCnt = 0
        var fCnt = 0
        assertEquals(fail, fail.onSuccess { sCnt++ })
        assertEquals(fail, fail.onFailure { fCnt++ })
        assertEquals(0, sCnt)
        assertEquals(1, fCnt)
    }
}