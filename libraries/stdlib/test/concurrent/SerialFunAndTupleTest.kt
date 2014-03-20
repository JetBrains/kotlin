package test.concurrent

import java.io.ObjectOutputStream
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import java.io.ObjectInputStream
import java.io.Serializable
import java.util.HashMap
import junit.framework.TestCase
import junit.framework.Assert

class Serial(val a : String) : java.lang.Object(), Serializable {
    override fun toString() = a
}

class SerialTest() : TestCase() {
    fun testMe() {
        val tuple = Triple("lala", "bbb", Serial("serial"))
        val op = { -> tuple.toString() }

        val baos = ByteArrayOutputStream()
        val oos = ObjectOutputStream(baos)
        oos.writeObject(op)
        oos.close()

        val bais = ByteArrayInputStream(baos.toByteArray())
        val ins = ObjectInputStream(bais)
        val ops = ins.readObject() as (() -> String)

        Assert.assertEquals(op (), ops())
    }

    fun testComplex() {
        val y = 12
        val op = { (x:Int) -> (x + y).toString() }

        val op2 : Int.(Int) -> String = { op(this + it) }

        val baos = ByteArrayOutputStream()
        val oos = ObjectOutputStream(baos)
        oos.writeObject(op2)
        oos.close()

        val bais = ByteArrayInputStream(baos.toByteArray())
        val ins = ObjectInputStream(bais)
        val ops = ins.readObject() as (Int.(Int) -> String)

        Assert.assertEquals("27", 5.ops(10))
    }
}
