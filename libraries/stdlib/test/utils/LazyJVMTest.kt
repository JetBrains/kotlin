package test.utils


import kotlin.*
import kotlin.test.*
import java.io.*
import org.junit.Test as test

class LazyJVMTest {

    test fun lazyInitializationForcedOnSerialization() {
        for(mode in listOf(LazyThreadSafetyMode.SYNCHRONIZED, LazyThreadSafetyMode.NONE)) {
            val lazy = lazy(mode) { "initialized" }
            assertFalse(lazy.valueCreated)
            val lazy2 = serializeAndDeserialize(lazy)
            assertTrue(lazy.valueCreated)
            assertTrue(lazy2.valueCreated)
            assertEquals(lazy.value, lazy2.value)
        }
    }


    private fun serializeAndDeserialize<T>(value: T): T {
        val outputStream = ByteArrayOutputStream()
        val objectOutputStream = ObjectOutputStream(outputStream)

        objectOutputStream.writeObject(value)
        objectOutputStream.close()
        outputStream.close()

        val inputStream = ByteArrayInputStream(outputStream.toByteArray())
        val inputObjectStream = ObjectInputStream(inputStream)
        return inputObjectStream.readObject() as T
    }

}