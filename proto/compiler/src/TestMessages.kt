import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Created by user on 7/8/16.
 */

import PersonMessage

fun testMessageSerialization() {
    val s = ByteArrayOutputStream()
    val outs = CodedOutputStream(s)
    val msg =
    msg.writeTo(outs)

    val ins = CodedInputStream(ByteArrayInputStream(s.toByteArray()))
    val readMsg = Person("", 0, "", arrayOf())
    readMsg.mergeFrom(ins)

    assert(msg == readMsg)
}

fun main(args: Array<String>) {
    testMessageSerialization()
    println("OK")
}
