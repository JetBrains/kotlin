@file:kotlin.jvm.JvmVersion
package test.concurrent

import java.io.ObjectOutputStream
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import java.io.ObjectInputStream
import java.io.Serializable
import junit.framework.TestCase
import org.junit.Assert

private class Serial(val name: String) : Serializable {
    override fun toString() = name
}

private data class DataType(val name: String, val value: Int, val percent: Double) : Serializable

class SerializableTest() : TestCase() {
    fun testClosure() {
        val tuple = Triple("Ivan", 12, Serial("serial"))
        val fn = { tuple.toString() }

        val byteOutputStream = ByteArrayOutputStream()
        val bytes = with(byteOutputStream) {
            val objectStream = ObjectOutputStream(byteOutputStream)
            objectStream.writeObject(fn)
            objectStream.close()
            toByteArray()
        }

        val byteInputStream = ByteArrayInputStream(bytes)
        val objectStream = ObjectInputStream(byteInputStream)
        val deserialized = objectStream.readObject() as (() -> String)

        Assert.assertEquals(fn(), deserialized())
    }

    fun testComplexClosure() {
        val y = 12
        val fn1 = { x: Int -> (x + y).toString() }
        val fn2: Int.(Int) -> String = { fn1(this + it) }

        val byteOutputStream = ByteArrayOutputStream()
        val bytes = with(byteOutputStream) {
            val objectStream = ObjectOutputStream(byteOutputStream)
            objectStream.writeObject(fn2)
            objectStream.close()
            toByteArray()
        }

        val byteInputStream = ByteArrayInputStream(bytes)
        val objectStream = ObjectInputStream(byteInputStream)
        val deserialized = objectStream.readObject() as (Int.(Int) -> String)

        Assert.assertEquals(5.fn2(10), 5.deserialized(10))
    }

    fun testDataClass() {
        val data = DataType("name", 176, 1.4)

        val byteOutputStream = ByteArrayOutputStream()
        val bytes = with(byteOutputStream) {
            val objectStream = ObjectOutputStream(byteOutputStream)
            objectStream.writeObject(data)
            objectStream.close()
            toByteArray()
        }

        val byteInputStream = ByteArrayInputStream(bytes)
        val objectStream = ObjectInputStream(byteInputStream)
        val deserialized = objectStream.readObject() as DataType

        Assert.assertEquals(data, deserialized)
    }
}
