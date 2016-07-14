import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Created by user on 7/8/16.
 */

fun testMessageSerialization() {
    // Messages work only with CodedStream classes, provided by ProtoKot-runtime library.
    // One can create CodedStream passing any instance of corresponding Stream from Java's library.
    val s = ByteArrayOutputStream()
    val outs = CodedOutputStream(s)

    // All messages are immutable. Use Builders for creating new messages
    val msg = Person.BuilderPerson()
            .setEmail("wtf@dasda.com")  // all setters return this builder, so you could chain modifiers in LINQ-style
            .setId(42)
            .setName("John Doe")
            .setPhones(arrayOf(         // repeated fields stored as Array<>, so use arrayOf() for creating repeated fields
                    Person.PhoneNumber.BuilderPhoneNumber()
                            .setNumber("342143-23423-42")
                            .setType(Person.PhoneType.HOME)
                            .build()
            ))
            .build()    // don't forget to call build() to produce message
    msg.writeTo(outs)

    // Now let's use output stream as input to read our message from it!
    var ins = CodedInputStream(ByteArrayInputStream(s.toByteArray()))

    // Create default instance of message
    var readMsg = Person.BuilderPerson().build()
    // Read in that message data from input stream.
    readMsg.mergeFrom(ins)

    // Note, that currently mergeFrom is the only way to mutate instance of message.
    // Don't rely on it, probably mergeFrom will be refactored lately to guarantee full immutability of mesages.

    // Better way to read a message:
    ins = CodedInputStream(ByteArrayInputStream(s.toByteArray()))
    readMsg = Person.BuilderPerson().readFrom(ins).build()
    assert(msg == readMsg)
}

fun main(args: Array<String>) {
    testMessageSerialization()
    println("OK")
}
