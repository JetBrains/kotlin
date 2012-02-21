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
        model.getClass("something.else.Aaa")
        model.getClass("something.else.Zzz")
        model.getClass("another.Cheese")

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
