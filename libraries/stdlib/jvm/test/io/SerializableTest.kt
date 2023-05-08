/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.io

import java.io.*
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
        val fn = @JvmSerializableLambda { tuple.toString() }
        val deserialized = serializeAndDeserialize(fn)

        assertEquals(fn(), deserialized())
    }

    @Test fun testComplexClosure() {
        val y = 12
        val fn1 = @JvmSerializableLambda { x: Int -> (x + y).toString() }
        val fn2: Int.(Int) -> String = @JvmSerializableLambda { fn1(this + it) }
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

public fun <T> serializeToByteArray(value: T): ByteArray {
    val outputStream = ByteArrayOutputStream()
    val objectOutputStream = ObjectOutputStream(outputStream)

    objectOutputStream.writeObject(value)
    objectOutputStream.close()
    outputStream.close()
    return outputStream.toByteArray()
}

public fun <T> deserializeFromByteArray(bytes: ByteArray): T {
    val inputStream = ByteArrayInputStream(bytes)
    val inputObjectStream = ObjectInputStream(inputStream)
    @Suppress("UNCHECKED_CAST")
    return inputObjectStream.readObject() as T
}

public fun <T> serializeAndDeserialize(value: T): T {
    val bytes = serializeToByteArray(value)
    return deserializeFromByteArray(bytes)
}

private fun hexToBytes(value: String): ByteArray = value.split(" ").map { Integer.parseInt(it, 16).toByte() }.toByteArray()

public fun <T> deserializeFromHex(value: String) = deserializeFromByteArray<T>(hexToBytes(value))

public fun <T> serializeToHex(value: T) =
    serializeToByteArray(value).joinToString(" ") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }

