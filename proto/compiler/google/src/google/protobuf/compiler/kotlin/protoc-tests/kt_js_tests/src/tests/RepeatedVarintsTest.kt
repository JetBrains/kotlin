package tests

import CodedInputStream
import MessageRepeatedVarints

object RepeatedVarintsTest {
    val builder = protoBuf.loadProtoFile("./js_messages/repeated_varints.proto")
    val JSMessageRepeatedVarints= builder.build("MessageRepeatedVarints")

    fun generateKtRepeatedVarints(): MessageRepeatedVarints {
        val int = Util.generateIntArray()
        val long = Util.generateLongArray()
        val sint = Util.generateIntArray()
        val slong = Util.generateLongArray()
        val bool = Util.generateBoolArray()
        val uint = Util.generateIntArray()
        val ulong = Util.generateLongArray()

        return MessageRepeatedVarints.BuilderMessageRepeatedVarints(
                int, long, sint, slong, bool, uint, ulong
        ).build()
    }

    fun generateJSRepeatedVarints(): dynamic {
        val int = Util.generateIntArray()
        val long = Util.generateIntArray()
        val sint = Util.generateIntArray()
        val slong = Util.generateIntArray()
        val bool = Util.generateBoolArray()
        val uint = Util.generateIntArray()
        val ulong = Util.generateIntArray()

        val MessageClass = JSMessageRepeatedVarints
        return js("new MessageClass(int, long, sint, slong, bool, uint, ulong)")
    }

    fun compareRepeatedVarints(kt: MessageRepeatedVarints, jvs: dynamic): Boolean {
        return Util.compareArrays(kt.int, jvs.int) &&
                Util.compareArrays(kt.long, jvs.long) &&
                Util.compareArrays(kt.sint, jvs.sint) &&
                Util.compareArrays(kt.slong, jvs.slong) &&
                Util.compareArrays(kt.bl, jvs.bl) &&
                Util.compareUIntArrays(kt.uint, jvs.uint) &&
                Util.compareULongArrays(kt.ulong, jvs.ulong)
    }

    fun ktToJsOnce() {
        val kt = generateKtRepeatedVarints()
        val outs = Util.getKtOutputStream(kt.getSizeNoTag())
        kt.writeTo(outs)

        val jvs = JSMessageRepeatedVarints.decode(outs.buffer)
        Util.assert(kt.errorCode == 0)
        Util.assert(compareRepeatedVarints(kt, jvs))
    }

    fun jsToKtOnce() {
        val jvs = generateJSRepeatedVarints()
        val byteBuffer = jvs.toBuffer()

        val ins = CodedInputStream(Util.JSBufferToByteArray(byteBuffer))
        val kt = MessageRepeatedVarints.BuilderMessageRepeatedVarints(
                    IntArray(0), LongArray(0), IntArray(0), LongArray(0), BooleanArray(0), IntArray(0), LongArray(0)
                ).parseFrom(ins).build()

        js("debugger")
        Util.assert(kt.errorCode == 0)
        Util.assert(compareRepeatedVarints(kt, jvs))
    }

    val testRuns = 1

    fun runTests() {
        for (i in 0..testRuns) {
            println("JavaScript -> Kotlin")
            jsToKtOnce()

            println()

            println("Kotlin -> JavaScript")
            ktToJsOnce()

        }
    }
}