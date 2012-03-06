package test.language

import junit.framework.TestCase
import java.util.Collection
import kotlin.test.*

class NullableCollectionsTest : TestCase() {

    fun testIterateOverNullCollectionsThrowsNPE() {
        val c: Collection<String>? = null

        // TODO currently this will throw a NPE
        // should it either be a compile error or handle nulls gracefully?
        failsWith<NullPointerException> {
            for (e in c) {
                println("Hey got $e")
            }
        }
    }
}
