package tests
import MessageVarints
import CodedInputStream

object VarintsTest {
    fun generateKtVarint(): MessageVarints {
        val int   = Util.nextInt()
        val long  = Util.nextLong()
        val sint  = Util.nextInt()
        val slong = Util.nextLong()
        val bl    = Util.nextBoolean()
        val uint  = Util.nextInt(0, Int.MAX_VALUE)
        val ulong = Util.nextLong(0, Long.MAX_VALUE)
        val enum  = MessageVarints.TestEnum.firstVal

        return MessageVarints.BuilderMessageVarints(
                int, long, sint, slong, bl, enum, uint, ulong
        ).build()
    }

    fun compareVarints(kt1: MessageVarints, kt2: MessageVarints): Boolean {
        return kt1.int == kt2.int &&
                kt1.long.toString() == kt2.long.toString() &&
                kt1.sint == kt2.sint &&
                kt1.slong.toString() == kt2.slong.toString() &&
                kt1.bl == kt2.bl &&
                kt1.uint == kt2.uint &&
                kt1.ulong.toString() == kt2.ulong.toString() &&
                kt1.enumField.id == kt2.enumField.id
    }

   fun ktToKtOnce() {
        val msg = generateKtVarint()
        val outs = Util.getKtOutputStream(msg.getSizeNoTag())
        msg.writeTo(outs)

        val ins = CodedInputStream(outs.buffer)
        val readMsg = MessageVarints.BuilderMessageVarints(0, 0L, 0, 0L, false, MessageVarints.TestEnum.firstVal, 0, 0L)
                .parseFrom(ins).build()

        Util.assert(readMsg.errorCode == 0)
        Util.assert(compareVarints(msg, readMsg))
    }

    val testRuns = 1000

    fun runTests() {
        for (i in 0..testRuns) {
            ktToKtOnce()
        }
    }
}