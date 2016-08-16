package kt_java_tests

import java_msg.RepeatedVarints
import CodedInputStream
import MessageRepeatedVarints
import java.io.ByteArrayOutputStream

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

    fun generateJvRepeatedVarints(): RepeatedVarints.MessageRepeatedVarints {
        val int = Util.generateIntArray()
        val long = Util.generateLongArray()
        val sint = Util.generateIntArray()
        val slong = Util.generateLongArray()
        val bool = Util.generateBoolArray()
        val uint = Util.generateIntArray()
        val ulong = Util.generateLongArray()

        return RepeatedVarints.MessageRepeatedVarints.newBuilder()
                .addAllInt(int.asIterable())
                .addAllLong(long.asIterable())
                .addAllSint(sint.asIterable())
                .addAllSlong(slong.asIterable())
                .addAllBl(bool.asIterable())
                .addAllUint(uint.asIterable())
                .addAllUlong(ulong.asIterable())
                .build()
    }

    fun compareRepeatedVarints(kt: MessageRepeatedVarints, jv: RepeatedVarints.MessageRepeatedVarints): Boolean {
        return Util.compareArrays(kt.int.asIterable(), jv.intList) &&
                Util.compareArrays(kt.long.asIterable(), jv.longList) &&
                Util.compareArrays(kt.sint.asIterable(), jv.sintList) &&
                Util.compareArrays(kt.slong.asIterable(), jv.slongList) &&
                Util.compareArrays(kt.bl.asIterable(), jv.blList) &&
                Util.compareArrays(kt.uint.asIterable(), jv.uintList) &&
                Util.compareArrays(kt.ulong.asIterable(), jv.ulongList)
    }

    fun ktToJavaOnce() {
        val kt = generateKtRepeatedVarints()
        val outs = Util.getKtOutputStream(kt.getSizeNoTag())

        kt.writeTo(outs)

        val ins = Util.KtOutputStreamToInputStream(outs)
        val jv = RepeatedVarints.MessageRepeatedVarints.parseFrom(ins)

        Util.assert(kt.errorCode == 0)
        Util.assert(compareRepeatedVarints(kt, jv))
    }

    fun jvToKtOnce() {
        val outs = ByteArrayOutputStream(10000000)

        val jv = generateJvRepeatedVarints()
        jv.writeTo(outs)
        val ins = CodedInputStream(outs.toByteArray())
        val kt = MessageRepeatedVarints.BuilderMessageRepeatedVarints(
                IntArray(0), LongArray(0), IntArray(0), LongArray(0), BooleanArray(0), IntArray(0), LongArray(0)
        ).parseFrom(ins).build()

        Util.assert(kt.errorCode == 0)
        Util.assert(compareRepeatedVarints(kt, jv))
    }

    val testRuns = 1000

    fun runTests() {
        for (i in 0..testRuns) {
            ktToJavaOnce()
            jvToKtOnce()
        }
    }
}