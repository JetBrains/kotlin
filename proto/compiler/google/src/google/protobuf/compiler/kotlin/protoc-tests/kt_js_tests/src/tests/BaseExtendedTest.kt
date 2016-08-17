package tests

import Base
import CodedInputStream
import Extended


object BaseExtendedTest {
    fun generateKtBaseMessage(): Base {
        val arrSize = Util.nextInt(1000)
        val arr = LongArray(arrSize)
        for (i in 0..(arrSize - 1)) {
            arr[i] = Util.nextLong()
        }

        val flag = if (Util.nextInt() % 2 == 0) false else true

        val int = Util.nextInt()

        return Base.BuilderBase(arr, flag, int).build()
    }

    fun generateKtExtendedMessage(): Extended {
        val arrSize = Util.nextInt(1000)
        val arr = LongArray(arrSize)
        for (i in 0..(arrSize - 1)) {
            arr[i] = Util.nextLong()
        }

        val flag = if (Util.nextInt() % 2 == 0) false else true

        val int = Util.nextInt()

        val long = Util.nextLong()

        val longsSize = Util.nextInt(1000)
        val longs = LongArray(longsSize)
        for (i in 0..(longsSize - 1)) {
            longs[i] = Util.nextLong()
        }

        return Extended.BuilderExtended(arr, longs, flag, long, int).build()
    }

    fun compareBases(kt: Base, jv: Base): Boolean {
        return kt.flag == jv.flag &&
                kt.int == jv.int &&
                Util.compareArrays(kt.arr.asIterable(), jv.arr.asIterable())
    }

    fun compareExtended(kt: Extended, jv: Extended): Boolean {
        return kt.flag == jv.flag &&
                kt.int == jv.int &&
                Util.compareArrays(kt.arr.asIterable(), jv.arr.asIterable()) &&
                Util.compareArrays(kt.arr_longs.asIterable(), jv.arr_longs.asIterable()) &&
                kt.long == jv.long
    }

    fun compareBaseExtended(kt1: Base, kt2: Extended): Boolean {
        return kt1.flag == kt2.flag &&
                kt1.int == kt2.int &&
                Util.compareArrays(kt1.arr.asIterable(), kt2.arr.asIterable())
    }

    fun baseToBaseOnce() {
        val msg = generateKtBaseMessage()
        val outs = Util.getKtOutputStream(msg.getSizeNoTag())
        msg.writeTo(outs)

        val ins = CodedInputStream(outs.buffer)
        val readMsg = Base.BuilderBase(LongArray(0), false, 0).parseFrom(ins).build()

        Util.assert(readMsg.errorCode == 0)
        Util.assert(compareBases(msg, readMsg))
    }

    fun extendedToExtendedOnce() {
        val msg = generateKtExtendedMessage()
        val outs = Util.getKtOutputStream(msg.getSizeNoTag())
        msg.writeTo(outs)

        val ins = CodedInputStream(outs.buffer)
        val readMsg = Extended.BuilderExtended(LongArray(0), LongArray(0), false, 0L, 0).parseFrom(ins).build()

        Util.assert(readMsg.errorCode == 0)
        Util.assert(compareExtended(msg, readMsg))
    }

    fun baseToExtendedOnce() {
        val msg = generateKtBaseMessage()
        val outs = Util.getKtOutputStream(msg.getSizeNoTag())
        msg.writeTo(outs)

        val ins = CodedInputStream(outs.buffer)
        val readMsg = Extended.BuilderExtended(LongArray(0), LongArray(0), false, 0L, 0).parseFrom(ins).build()

        Util.assert(readMsg.errorCode == 0)
        Util.assert(compareBaseExtended(msg, readMsg))
    }

    fun extendedToBaseOnce() {
        val msg = generateKtExtendedMessage()
        val outs = Util.getKtOutputStream(msg.getSizeNoTag())
        msg.writeTo(outs)

        val ins = CodedInputStream(outs.buffer)
        val readMsg = Base.BuilderBase(LongArray(0), false, 0).parseFrom(ins).build()

        Util.assert(readMsg.errorCode == 0)
        Util.assert(compareBaseExtended(readMsg, msg))
    }

    val testRuns = 1000
    fun runTests() {
        for (i in 0..testRuns) {
            baseToBaseOnce()
            extendedToExtendedOnce()

            baseToExtendedOnce()
//            extendedToBaseOnce()
        }
    }
}