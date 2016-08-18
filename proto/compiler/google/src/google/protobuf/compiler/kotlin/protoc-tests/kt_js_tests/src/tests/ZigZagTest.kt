package tests
import MessageZigZag
import CodedInputStream

object ZigZagTest {
    fun generateKtZigZag(): MessageZigZag {
        val int = Util.nextInt()
        val long = Util.nextLong()

        return MessageZigZag.BuilderMessageZigZag(int, long.toLong()).build()
    }

    fun compareZigZags(kt1: MessageZigZag, kt2: MessageZigZag): Boolean {
        return kt1.int == kt2.int &&
                kt1.long == kt2.long
    }

    fun ktToKtOnce() {
        val msg = generateKtZigZag()
        val outs = Util.getKtOutputStream(msg.getSizeNoTag())
        msg.writeTo(outs)

        val ins = CodedInputStream(outs.buffer)
        val readMsg = MessageZigZag.BuilderMessageZigZag(0, 0L).parseFrom(ins).build()

        Util.assert(readMsg.errorCode == 0)
        Util.assert(compareZigZags(msg, readMsg))
    }

    val testRuns = 1000

    fun runTests() {
        for (i in 0..testRuns) {
            ktToKtOnce()
        }
    }
}