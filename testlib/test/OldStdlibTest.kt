package test.collections

import std.*
import std.io.*
import std.util.*
import stdhack.test.*
import java.util.*
import java.io.*

class OldStdlibTest() : TestSupport() {
    fun testCollectionEmpty() {
        assertNot {
            Arrays.asList(0, 1, 2)?.empty ?: false
        }
    }

    fun testCollectionSize() {
        assert {
            Arrays.asList(0, 1, 2)?.size == 3
        }
    }

    fun testInputStreamIterator() {
        val x = ByteArray (10)

        for(index in 0..9) {
            x [index] = index.toByte()
        }

        for(b in x.inputStream) {
           System.out?.println(b)
        }
    }
}
