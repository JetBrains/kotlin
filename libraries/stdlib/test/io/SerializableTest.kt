@file:kotlin.jvm.JvmVersion
package test.io

import java.io.*
import org.junit.Test
import kotlin.test.*

private class Serial(val name: String) : Serializable {
    override fun toString() = name
}

private data class DataType(val name: String, val value: Int, val percent: Double) : Serializable

private enum class EnumSingleton { INSTANCE }
private object ObjectSingleton : Serializable {
    private fun readResolve(): Any = ObjectSingleton
}

private class OldSchoolSingleton private constructor() : Serializable {
    private fun readResolve(): Any = INSTANCE

    companion object {
        val INSTANCE = OldSchoolSingleton()
    }
}


class SerializableTest {
    @Test fun testClosure() {
        val tuple = Triple("Ivan", 12, Serial("serial"))
        val fn = { tuple.toString() }
        val deserialized = serializeAndDeserialize(fn)

        assertEquals(fn(), deserialized())
    }

    @Test fun testComplexClosure() {
        val y = 12
        val fn1 = { x: Int -> (x + y).toString() }
        val fn2: Int.(Int) -> String = { fn1(this + it) }
        val deserialized = serializeAndDeserialize(fn2)

        assertEquals(5.fn2(10), 5.deserialized(10))
    }

    @Test fun testDataClass() {
        val data = DataType("name", 176, 1.4)
        val deserialized = serializeAndDeserialize(data)

        assertEquals(data, deserialized)
    }

    @Test fun testSingletons() {
        assertTrue(EnumSingleton.INSTANCE === serializeAndDeserialize(EnumSingleton.INSTANCE))
        assertTrue(OldSchoolSingleton.INSTANCE === serializeAndDeserialize(OldSchoolSingleton.INSTANCE))
        assertTrue(ObjectSingleton === serializeAndDeserialize(ObjectSingleton))
    }
}

public fun <T> serializeAndDeserialize(value: T): T {
    val outputStream = ByteArrayOutputStream()
    val objectOutputStream = ObjectOutputStream(outputStream)

    objectOutputStream.writeObject(value)
    objectOutputStream.close()
    outputStream.close()

    val inputStream = ByteArrayInputStream(outputStream.toByteArray())
    val inputObjectStream = ObjectInputStream(inputStream)
    @Suppress("UNCHECKED_CAST")
    return inputObjectStream.readObject() as T
}

private fun hexToBytes(value: String): ByteArray = value.split(" ").map { Integer.parseInt(it, 16).toByte() }.toByteArray()

public fun <T> deserializeFromHex(value: String) = hexToBytes(value).let {
    val inputStream = ByteArrayInputStream(it)
    val inputObjectStream = ObjectInputStream(inputStream)
    @Suppress("UNCHECKED_CAST")
    inputObjectStream.readObject() as T
}
