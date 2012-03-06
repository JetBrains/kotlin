package test.language

import junit.framework.TestCase
import kotlin.test.*

import org.jetbrains.kotlin.support.*

fun localUseWorks(): Unit {
    val c = javaClass<Runnable>()
    println("class is $c")
}

class JavaClassTest : TestCase() {

    fun testJavaClass() {
        localUseWorks()

        // TODO this function fails!
        // see KT-1515
        // loadAsserter()
    }
}

