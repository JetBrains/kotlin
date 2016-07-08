import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Created by user on 7/8/16.
 */

fun testMessageSerialization() {
    val s = ByteArrayOutputStream()
    val outs = CodedOutputStream(s)
    val msg = PersonMessage(name = "John Doe", id = 42, hasCat = true)
    msg.writeTo(outs)

    val ins = CodedInputStream(ByteArrayInputStream(s.toByteArray()))
    val readMsg = PersonMessage("", 0, false)
    readMsg.readFrom(ins)

    assert(msg == readMsg)
}

fun main(args: Array<String>) {
    testMessageSerialization()
    println("OK")
}
