package kt_java_tests

import java_msg.RepeatedZigzag
import CodedInputStream
import MessageRepeatedZigZag
import java.io.ByteArrayOutputStream

object RepeatedZigZag {
    fun generateKtRepeatedZigZag(): MessageRepeatedZigZag {
        val int = Util.generateIntArray()
        val long = Util.generateLongArray()
        return MessageRepeatedZigZag.BuilderMessageRepeatedZigZag(int, long).build()
    }

    fun generateJvRepatedZigZag(): RepeatedZigzag.MessageRepeatedZigZag {
        val int = Util.generateIntArray()
        val long = Util.generateLongArray()
        return RepeatedZigzag.MessageRepeatedZigZag.newBuilder()
                .addAllInt(int.asIterable())
                .addAllLong(long.asIterable())
                .build()
    }

    fun compareRepeatedZigZags(kt: MessageRepeatedZigZag, jv: RepeatedZigzag.MessageRepeatedZigZag): Boolean {
        return Util.compareArrays(kt.int.asIterable(), jv.intList) &&
                Util.compareArrays(kt.long.asIterable(), jv.longList)
    }

    fun ktToJavaOnce() {
        val kt = generateKtRepeatedZigZag()
        val outs = Util.getKtOutputStream(kt.getSizeNoTag())

        kt.writeTo(outs)

        val ins = Util.KtOutputStreamToInputStream(outs)
        val jv = RepeatedZigzag.MessageRepeatedZigZag.parseFrom(ins)

        Util.assert(kt.errorCode == 0)
        Util.assert(compareRepeatedZigZags(kt, jv))
    }

    fun jvToKtOnce() {
        val outs = ByteArrayOutputStream(10000000)

        val jv = generateJvRepatedZigZag()
        jv.writeTo(outs)
        val ins = CodedInputStream(outs.toByteArray())
        val kt = MessageRepeatedZigZag.BuilderMessageRepeatedZigZag(IntArray(0), LongArray(0)).parseFrom(ins).build()

        Util.assert(kt.errorCode == 0)
        Util.assert(compareRepeatedZigZags(kt, jv))
    }

    val testRuns = 1000

    fun runTests() {
        for (i in 0..testRuns) {
            ktToJavaOnce()
            jvToKtOnce()
        }
    }
}