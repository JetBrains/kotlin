import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Created by user on 7/8/16.
 */

fun testMessageSerialization() {
    val s = ByteArrayOutputStream()
    val outs = CodedOutputStream(s)
    val msg = Person(
            name = "John Doe",
            id = 42,
            email = "wtf@dsada.com",
            phones = arrayOf (
                    Person.PhoneNumber("8-800-555-35-35", Person.PhoneType.WORK),
                    Person.PhoneNumber("228-322", Person.PhoneType.HOME)
            )
    )
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
