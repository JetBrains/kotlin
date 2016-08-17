package tests
import CodedInputStream
import MessageRepeatedZigZag

object RepeatedZigZagTest {
    fun generateKtRepeatedZigZag(): MessageRepeatedZigZag {
        val int = Util.generateIntArray()
        val long = Util.generateLongArray()
        return MessageRepeatedZigZag.BuilderMessageRepeatedZigZag(int, long).build()
    }

    fun compareRepeatedZigZags(kt1: MessageRepeatedZigZag, kt2: MessageRepeatedZigZag): Boolean {
        return Util.compareArrays(kt1.int.asIterable(), kt2.int.asIterable()) &&
                Util.compareArrays(kt1.long.asIterable(), kt2.long.asIterable())
    }

    fun ktToKtOnce() {
        val msg = generateKtRepeatedZigZag()
        val outs = Util.getKtOutputStream(msg.getSizeNoTag())
        msg.writeTo(outs)

        val ins = CodedInputStream(outs.buffer)
        val readMsg = MessageRepeatedZigZag.BuilderMessageRepeatedZigZag(IntArray(0), LongArray(0)).parseFrom(ins).build()

        Util.assert(readMsg.errorCode == 0)
        Util.assert(compareRepeatedZigZags(msg, readMsg))
    }

    val testRuns = 1000

    fun runTests() {
        for (i in 0..testRuns) {
            ktToKtOnce()
        }
    }
}