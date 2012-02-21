package test.model

import std.*
import std.util.*

import org.jetbrains.kotlin.model.*

import junit.framework.TestCase
import junit.framework.Assert
import junit.framework.Assert.assertEquals

class ModelTest : TestCase() {
    val model = KModel()

    fun testGetClassViaPackage() {
        val p = model.getPackage("foo.bar")
        val c = p.getClass("Cheese")

        println("Got class: $c")

        // TODO should use the ktest library really for nicer asserts
        Assert.assertEquals("foo.bar.Cheese", c.name)
        Assert.assertEquals("foo.bar", c.packageName)
        Assert.assertEquals("Cheese", c.simpleName)
    }

    fun testGetClassViaQualifiedName() {
        val c = model.getClass("something.else.Foo")

        println("Got class: $c")

        // TODO should use the ktest library really for nicer asserts
        Assert.assertEquals("something.else.Foo", c.name)
        Assert.assertEquals("something.else", c.packageName)
        Assert.assertEquals("Foo", c.simpleName)
    }

    fun testModelWalk() {
        model.getClass("something.else.Aaa")
        model.getClass("something.else.Zzz")

        // now lets iterate through the model
        println("Walking the model...")
        for (p in model.packages) {
            println("Package ${p.name}")
            for (c in p.classes) {
                println("    ${c.simpleName}")
            }
            println()
        }
    }
}
