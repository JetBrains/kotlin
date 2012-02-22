package test.kotlin.doc

import std.*
import std.util.*

import org.jetbrains.kotlin.model.*

import junit.framework.TestCase

// TODO should use the ktest library really for nicer asserts
import junit.framework.Assert.assertEquals
import org.jetbrains.kotlin.doc.KDocProcessor
import java.io.File

class ModelTest : TestCase() {
    val model = KModel()

    fun testGetClassViaPackage() {
        val p = model.getPackage("foo.bar")
        val c = p.getClass("Cheese")

        println("Got class: $c")

        assertEquals("foo.bar.Cheese", c.name)
        assertEquals("foo.bar", c.packageName)
        assertEquals("Cheese", c.simpleName)
    }

    fun testGetClassViaQualifiedName() {
        val c = model.getClass("something.else.Foo")

        println("Got class: $c")

        assertEquals("something.else.Foo", c.name)
        assertEquals("something.else", c.packageName)
        assertEquals("Foo", c.simpleName)
    }

    fun testModelWalk() {
        val a = model.getClass("something.else.Aaa")
        val z = model.getClass("something.else.Zzz")
        val c = model.getClass("another.Cheese")

        c.methods.add(KMethod("addFoo", a, "add some foos"))
        val m1 = KMethod("addZzzz", z, "add some zzz")
        m1.parameters.add(KParameter("myz", z))
        c.methods.add(m1)

        // now lets iterate through the model
        println("Walking the model...")
        for (p in model.packages) {
            println("Package ${p.name}")
            for (c in p.classes) {
                println("    ${c.simpleName}")
            }
            println()
        }

        val processor = KDocProcessor(model, File("target/test-data/ModelTest"))
        processor.execute()
    }
}
