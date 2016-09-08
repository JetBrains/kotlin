package tests

import CodedInputStream
import MessageRepeatedVarints

object RepeatedVarintsTest {
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

    fun compareRepeatedVarints(kt1: MessageRepeatedVarints, kt2: MessageRepeatedVarints): Boolean {
        return Util.compareArrays(kt1.int.asIterable(), kt2.int.asIterable()) &&
                Util.compareArrays(kt1.long.asIterable(), kt2.long.asIterable()) &&
                Util.compareArrays(kt1.sint.asIterable(), kt2.sint.asIterable()) &&
                Util.compareArrays(kt1.slong.asIterable(), kt2.slong.asIterable()) &&
                Util.compareArrays(kt1.bl.asIterable(), kt2.bl.asIterable()) &&
                Util.compareArrays(kt1.uint.asIterable(), kt2.uint.asIterable()) &&
                Util.compareArrays(kt1.ulong.asIterable(), kt2.ulong.asIterable())
    }

    fun ktToKtOnce() {
        val msg = generateKtRepeatedVarints()
        val outs = Util.getKtOutputStream(msg.getSizeNoTag())
        msg.writeTo(outs)

        val ins = CodedInputStream(outs.buffer)
        val readMsg = MessageRepeatedVarints.BuilderMessageRepeatedVarints(
                IntArray(0), LongArray(0), IntArray(0), LongArray(0), BooleanArray(0), IntArray(0), LongArray(0)
        ).parseFrom(ins).build()

        Util.assert(readMsg.errorCode == 0)
        Util.assert(compareRepeatedVarints(msg, readMsg))
    }

    val testRuns = 1000

    fun runTests() {
        for (i in 0..testRuns) {
            ktToKtOnce()
        }
    }
}