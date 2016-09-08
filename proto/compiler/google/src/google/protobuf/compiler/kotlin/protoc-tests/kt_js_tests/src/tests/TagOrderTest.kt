package tests

import CodedInputStream
import TagOrder
import TagOrderShuffled

object TagOrderTest {
    fun generateKtTag(): TagOrder {
        val int = Util.nextInt()
        val slong_array = Util.generateLongArray()
        val sint = Util.nextInt()
        val enum = TagOrder.Enum.fromIntToEnum(Util.nextInt(2))
        return TagOrder.BuilderTagOrder(int, sint, slong_array, enum).build()
    }

    fun generateKtTagShuffled(): TagOrderShuffled {
        val int = Util.nextInt()
        val slong_array = Util.generateLongArray()
        val sint = Util.nextInt()
        val enum = TagOrderShuffled.Enum.fromIntToEnum(Util.nextInt(2))

        return TagOrderShuffled.BuilderTagOrderShuffled(int, sint, slong_array, enum).build()
    }

    fun compareTagAndShuffledTag(kt1: TagOrder, kt2: TagOrderShuffled): Boolean {
        return kt1.enum_field.id == kt2.enum_field.id &&
                kt1.int == kt2.int &&
                kt1.sint == kt2.sint &&
                Util.compareArrays(kt1.slong_array.asIterable(), kt2.slong_array.asIterable())
    }

    fun tagToShuffledOnce() {
        val msg = generateKtTag()
        val outs = Util.getKtOutputStream(msg.getSizeNoTag())
        msg.writeTo(outs)

        val ins = CodedInputStream(outs.buffer)
        val readMsg = TagOrderShuffled.BuilderTagOrderShuffled(0, 0, LongArray(0), TagOrderShuffled.Enum.first_val)
                .parseFrom(ins).build()

        Util.assert(readMsg.errorCode == 0)
        Util.assert(compareTagAndShuffledTag(msg, readMsg))
    }

    fun shuffledToTagOnce() {
        val msg = generateKtTagShuffled()
        val outs = Util.getKtOutputStream(msg.getSizeNoTag())
        msg.writeTo(outs)

        val ins = CodedInputStream(outs.buffer)
        val readMsg = TagOrder.BuilderTagOrder(0, 0, LongArray(0), TagOrder.Enum.first_val)
                .parseFrom(ins).build()

        Util.assert(readMsg.errorCode == 0)
        Util.assert(compareTagAndShuffledTag(readMsg, msg))
    }

    val testRuns = 1000
    fun runTests() {
        for (i in 0..testRuns) {
            tagToShuffledOnce()
            shuffledToTagOnce()
        }
    }
}