package tests

import java_msg.Varints
import main.kotlin.CodedInputStream
import main.kotlin.MessageVarints
import java.io.ByteArrayOutputStream
import java.util.*

object VarintsTest {
    fun generateKtVarint(): MessageVarints {
        val int = RandomGen.rnd.nextInt()
        val long = RandomGen.rnd.nextLong()
        val sint = RandomGen.rnd.nextInt()
        val slong = RandomGen.rnd.nextLong()
        val bl = RandomGen.rnd.nextBoolean()
        val uint = RandomGen.rnd.nextInt()
        val ulong = RandomGen.rnd.nextLong()
        val enum = MessageVarints.TestEnum.fromIntToTestEnum(RandomGen.rnd.nextInt(2))

        return MessageVarints.BuilderMessageVarints(
                int, long, sint, slong, bl, enum, uint, ulong
            ).build()
    }

    fun generateMaxKtVarint(): MessageVarints {
        val int = Int.MAX_VALUE
        val long = Long.MAX_VALUE
        val sint = Int.MAX_VALUE
        val slong = Long.MAX_VALUE
        val bl = false
        val uint = Int.MAX_VALUE
        val ulong = Long.MAX_VALUE
        val enum = MessageVarints.TestEnum.fromIntToTestEnum(RandomGen.rnd.nextInt(2))

        return MessageVarints.BuilderMessageVarints(
                int, long, sint, slong, bl, enum, uint, ulong
        ).build()
    }

    fun generateMinKtVarint(): MessageVarints {
        val int = Int.MIN_VALUE
        val long = Long.MIN_VALUE
        val sint = Int.MIN_VALUE
        val slong = Long.MIN_VALUE
        val uint = Int.MIN_VALUE
        val ulong = Long.MIN_VALUE
        val bl = false
        val enum = MessageVarints.TestEnum.fromIntToTestEnum(RandomGen.rnd.nextInt(2))

        return MessageVarints.BuilderMessageVarints(
                int, long, sint, slong, bl, enum, uint, ulong
        ).build()
    }

    fun generateJvVarint(): Varints.MessageVarints {
        val int = RandomGen.rnd.nextInt()
        val long = RandomGen.rnd.nextLong()
        val sint = RandomGen.rnd.nextInt()
        val slong = RandomGen.rnd.nextLong()
        val bl = RandomGen.rnd.nextBoolean()
        val uint = RandomGen.rnd.nextInt()
        val ulong = RandomGen.rnd.nextLong()
        val enum = Varints.MessageVarints.TestEnum.forNumber(RandomGen.rnd.nextInt(2))

        return Varints.MessageVarints.newBuilder()
                .setInt(int)
                .setLong(long)
                .setSint(sint)
                .setSlong(slong)
                .setBl(bl)
                .setUint(uint)
                .setUlong(ulong)
                .setEnumField(enum)
                .build()
    }

    fun generateMaxJvVarint(): Varints.MessageVarints {
        val int = Int.MAX_VALUE
        val long = Long.MAX_VALUE
        val sint = Int.MAX_VALUE
        val slong = Long.MAX_VALUE
        val bl = false
        val uint = Int.MAX_VALUE
        val ulong = Long.MAX_VALUE
        val enum = Varints.MessageVarints.TestEnum.forNumber(RandomGen.rnd.nextInt(2))

        return Varints.MessageVarints.newBuilder()
                .setInt(int)
                .setLong(long)
                .setSint(sint)
                .setSlong(slong)
                .setBl(bl)
                .setUint(uint)
                .setUlong(ulong)
                .setEnumField(enum)
                .build()
    }

    fun generateMinJvVarint(): Varints.MessageVarints {
        val int = Int.MIN_VALUE
        val long = Long.MIN_VALUE
        val sint = Int.MIN_VALUE
        val slong = Long.MIN_VALUE
        val uint = Int.MIN_VALUE
        val ulong = Long.MIN_VALUE
        val bl = false
        val enum = Varints.MessageVarints.TestEnum.forNumber(RandomGen.rnd.nextInt(2))

        return Varints.MessageVarints.newBuilder()
                .setInt(int)
                .setLong(long)
                .setSint(sint)
                .setSlong(slong)
                .setBl(bl)
                .setUint(uint)
                .setUlong(ulong)
                .setEnumField(enum)
                .build()
    }

    fun compareVarints(kt: MessageVarints, jv: Varints.MessageVarints): Boolean {
        return kt.int == jv.int &&
                kt.long == jv.long &&
                kt.sint == jv.sint &&
                kt.slong == jv.slong &&
                kt.bl == jv.bl &&
                kt.uint == jv.uint &&
                kt.ulong == jv.ulong &&
                kt.enumField.id == jv.enumField.number
    }

    fun ktToJavaOnce() {
        val kt = generateKtVarint()
        val outs = Util.getKtOutputStream(kt.getSizeNoTag())
        kt.writeTo(outs)

        val ins = Util.KtOutputStreamToInputStream(outs)
        val jv = Varints.MessageVarints.parseFrom(ins)

        Util.assert(kt.errorCode == 0)
        Util.assert(compareVarints(kt, jv))
    }

    fun jvToKtOnce() {
        val outs = ByteArrayOutputStream(10000000)

        val jv = generateJvVarint()
        jv.writeTo(outs)
        val ins = CodedInputStream(outs.toByteArray())
        val kt = MessageVarints.BuilderMessageVarints(
                0, 0, 0, 0, false, MessageVarints.TestEnum.firstVal, 0, 0
            ).parseFrom(ins).build()

        Util.assert(kt.errorCode == 0)
        Util.assert(compareVarints(kt, jv))
    }

    fun ktToJavaOnceMaxValues() {
        val kt = generateMaxKtVarint()
        val outs = Util.getKtOutputStream(kt.getSizeNoTag())
        kt.writeTo(outs)

        val ins = Util.KtOutputStreamToInputStream(outs)
        val jv = Varints.MessageVarints.parseFrom(ins)

        Util.assert(kt.errorCode == 0)
        Util.assert(compareVarints(kt, jv))
    }

    fun ktToJavaOnceMinValues() {
        val kt = generateMinKtVarint()
        val outs = Util.getKtOutputStream(kt.getSizeNoTag())
        kt.writeTo(outs)

        val ins = Util.KtOutputStreamToInputStream(outs)
        val jv = Varints.MessageVarints.parseFrom(ins)

        Util.assert(kt.errorCode == 0)
        Util.assert(compareVarints(kt, jv))
    }

    fun jvToKtOnceMaxValues() {
        val outs = ByteArrayOutputStream(10000000)

        val jv = generateMaxJvVarint()
        jv.writeTo(outs)
        val ins = CodedInputStream(outs.toByteArray())
        val kt = MessageVarints.BuilderMessageVarints(
                0, 0, 0, 0, false, MessageVarints.TestEnum.firstVal, 0, 0
        ).parseFrom(ins).build()

        Util.assert(kt.errorCode == 0)
        Util.assert(compareVarints(kt, jv))
    }

    fun jvToKtOnceMinValues() {
        val outs = ByteArrayOutputStream(10000000)

        val jv = generateMinJvVarint()
        jv.writeTo(outs)
        val ins = CodedInputStream(outs.toByteArray())
        val kt = MessageVarints.BuilderMessageVarints(
                0, 0, 0, 0, false, MessageVarints.TestEnum.firstVal, 0, 0
        ).parseFrom(ins).build()

        Util.assert(kt.errorCode == 0)
        Util.assert(compareVarints(kt, jv))
    }

    val testRuns = 1000

    fun runTests() {
        for (i in 0..testRuns) {
            ktToJavaOnce()
            jvToKtOnce()

            ktToJavaOnceMaxValues()
            jvToKtOnceMaxValues()

            ktToJavaOnceMinValues()
            jvToKtOnceMinValues()
        }
    }
}