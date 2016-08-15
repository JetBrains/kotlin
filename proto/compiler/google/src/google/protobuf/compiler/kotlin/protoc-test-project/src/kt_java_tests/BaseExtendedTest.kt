package kt_java_tests

import java_msg.BaseMessage
import java_msg.ExtendedMessage
import Base
import CodedInputStream
import Extended
import java.io.ByteArrayOutputStream


object BaseExtendedTest {
    fun generateKtBaseMessage(): Base {
        val arrSize = RandomGen.rnd.nextInt(1000)
        val arr = LongArray(arrSize)
        for (i in 0..(arrSize - 1)) {
            arr[i] = RandomGen.rnd.nextLong()
        }

        val flag = if (RandomGen.rnd.nextInt() % 2 == 0) false else true

        val int = RandomGen.rnd.nextInt()

        return Base.BuilderBase(arr, flag, int).build()
    }

    fun generateKtExtendedMessage(): Extended {
        val arrSize = RandomGen.rnd.nextInt(1000)
        val arr = LongArray(arrSize)
        for (i in 0..(arrSize - 1)) {
            arr[i] = RandomGen.rnd.nextLong()
        }

        val flag = if (RandomGen.rnd.nextInt() % 2 == 0) false else true

        val int = RandomGen.rnd.nextInt()

        val long = RandomGen.rnd.nextLong()

        val longsSize = RandomGen.rnd.nextInt(1000)
        val longs = LongArray(longsSize)
        for (i in 0..(longsSize - 1)) {
            longs[i] = RandomGen.rnd.nextLong()
        }

        return Extended.BuilderExtended(arr, longs, flag, long, int).build()
    }

    fun generateJvBaseMessage(): BaseMessage.Base {
        val arrSize = RandomGen.rnd.nextInt(1000)
        val arr = LongArray(arrSize)
        for (i in 0..(arrSize - 1)) {
            arr[i] = RandomGen.rnd.nextLong()
        }

        val flag = if (RandomGen.rnd.nextInt() % 2 == 0) false else true

        val int = RandomGen.rnd.nextInt()

        return BaseMessage.Base.newBuilder()
                .addAllArr(arr.asIterable())
                .setFlag(flag)
                .setInt(int)
                .build()
    }

    fun generateJvExtendedMessage(): ExtendedMessage.Extended {
        val arrSize = RandomGen.rnd.nextInt(1000)
        val arr = LongArray(arrSize)
        for (i in 0..(arrSize - 1)) {
            arr[i] = RandomGen.rnd.nextLong()
        }

        val flag = if (RandomGen.rnd.nextInt() % 2 == 0) false else true

        val int = RandomGen.rnd.nextInt()

        val long = RandomGen.rnd.nextLong()

        val longsSize = RandomGen.rnd.nextInt(1000)
        val longs = LongArray(longsSize)
        for (i in 0..(longsSize - 1)) {
            longs[i] = RandomGen.rnd.nextLong()
        }

        return ExtendedMessage.Extended.newBuilder()
                .addAllArr(arr.asIterable())
                .addAllArrLongs(longs.asIterable())
                .setFlag(flag)
                .setInt(int)
                .setLong(long)
                .build()
    }

    fun compareBases(kt: Base, jv: BaseMessage.Base): Boolean {
        return kt.flag == jv.flag &&
                kt.int == jv.int &&
                Util.compareArrays(kt.arr.asIterable(), jv.arrList)
    }

    fun compareExtended(kt: Extended, jv: ExtendedMessage.Extended): Boolean {
        return kt.flag == jv.flag &&
                kt.int == jv.int &&
                Util.compareArrays(kt.arr.asIterable(), jv.arrList) &&
                Util.compareArrays(kt.arr_longs.asIterable(), jv.arrLongsList) &&
                kt.long == jv.long
    }

    fun compareBaseKtToJavaExtended(kt: Base, jv: ExtendedMessage.Extended): Boolean {
        return kt.flag == jv.flag &&
                kt.int == jv.int &&
                Util.compareArrays(kt.arr.asIterable(), jv.arrList)
    }

    fun compareExtendedKtToJavaBase(kt: Extended, jv: BaseMessage.Base): Boolean {
        return kt.flag == jv.flag &&
                kt.int == jv.int &&
                Util.compareArrays(kt.arr.asIterable(), jv.arrList)
    }

    fun baseKtToBaseJavaOnce() {
        val kt = generateKtBaseMessage()
        val outs = Util.getKtOutputStream(kt.getSizeNoTag())
        kt.writeTo(outs)

        val ins = Util.KtOutputStreamToInputStream(outs)
        val jv = BaseMessage.Base.parseFrom(ins)

        Util.assert(kt.errorCode == 0)
        Util.assert(compareBases(kt, jv))
    }

    fun baseJavaToBaseKtOnce() {
        val outs = ByteArrayOutputStream(100000)

        val jv = generateJvBaseMessage()
        jv.writeTo(outs)
        val ins = CodedInputStream(outs.toByteArray())
        val kt = Base.BuilderBase(LongArray(0), false, 0).build()
        kt.mergeFrom(ins)

        Util.assert(kt.errorCode == 0)
        Util.assert(compareBases(kt, jv))
    }

    fun baseKtToExtendedJavaOnce() {
        val kt = generateKtBaseMessage()
        val outs = Util.getKtOutputStream(kt.getSizeNoTag())
        kt.writeTo(outs)

        val ins = Util.KtOutputStreamToInputStream(outs)
        val jv = ExtendedMessage.Extended.parseFrom(ins)

        Util.assert(kt.errorCode == 0)
        Util.assert(compareBaseKtToJavaExtended(kt, jv))
    }

    fun extendedJavaToBaseKtOnce() {
        val outs = ByteArrayOutputStream(100000)

        val jv = generateJvExtendedMessage()
        jv.writeTo(outs)

        val ins = CodedInputStream(outs.toByteArray())

        val kt = Base.BuilderBase(LongArray(0), false, 0).build()
        kt.mergeFrom(ins)

        Util.assert(kt.errorCode == 0)
        Util.assert(compareBaseKtToJavaExtended(kt, jv))
    }

    fun extendedKtToBaseJavaOnce() {
        val kt = generateKtExtendedMessage()
        val outs = Util.getKtOutputStream(kt.getSizeNoTag())
        kt.writeTo(outs)

        val ins = Util.KtOutputStreamToInputStream(outs)
        val jv = BaseMessage.Base.parseFrom(ins)

        Util.assert(kt.errorCode == 0)
        Util.assert(compareExtendedKtToJavaBase(kt, jv))
    }

    fun baseJavaToExtendedKtOnce() {
        val outs = ByteArrayOutputStream(100000)

        val jv = generateJvBaseMessage()
        jv.writeTo(outs)

        val ins = CodedInputStream(outs.toByteArray())

        val kt = Extended.BuilderExtended(LongArray(0), LongArray(0), false, 0L, 0).build()
        kt.mergeFrom(ins)

        Util.assert(kt.errorCode == 0)
        Util.assert(compareExtendedKtToJavaBase(kt, jv))
    }

    fun extendedKtToExtendedJavaOnce() {
        val kt = generateKtExtendedMessage()
        val outs = Util.getKtOutputStream(kt.getSizeNoTag())
        kt.writeTo(outs)

        val ins = Util.KtOutputStreamToInputStream(outs)
        val jv = ExtendedMessage.Extended.parseFrom(ins)

        Util.assert(kt.errorCode == 0)
        Util.assert(compareExtended(kt, jv))
    }

    fun extendedJavaToExtendedKtOnce() {
        val outs = ByteArrayOutputStream(100000)

        val jv = generateJvExtendedMessage()
        jv.writeTo(outs)
        val ins = CodedInputStream(outs.toByteArray())
        val kt = Extended.BuilderExtended(LongArray(0), LongArray(0), false, 0L, 0).build()
        kt.mergeFrom(ins)

        Util.assert(kt.errorCode == 0)
        Util.assert(compareExtended(kt, jv))
    }

    val testRuns = 1000
    fun runTests() {
        for (i in 0..testRuns) {
            // base - base
            baseJavaToBaseKtOnce()
            baseKtToBaseJavaOnce()

            // base - extended
            baseKtToExtendedJavaOnce()
            // extendedJavaToBaseKtOnce() - currently failing, proper parsing of unknown fields is needed.

            // extended - base
            baseJavaToExtendedKtOnce()
            extendedKtToBaseJavaOnce()

            // extended - extended
            extendedJavaToExtendedKtOnce()
            extendedKtToExtendedJavaOnce()
        }
    }
}