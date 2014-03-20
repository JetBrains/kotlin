package regressions

// TODO comment out the next line to reproduce KT-1617
//import kotlin.util.map

import java.io.File

import junit.framework.TestCase

class Kt1617Test: TestCase() {
    fun testMapFunction() {
        val coll: Collection<String> = arrayListOf("foo", "bar")

        val files =  coll.map{ File(it) }

        println("Found files: $files")
    }
}