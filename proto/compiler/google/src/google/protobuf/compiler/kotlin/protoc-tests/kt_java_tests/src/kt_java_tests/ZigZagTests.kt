package kt_java_tests

import java_msg.Zigzag
import CodedInputStream
import MessageZigZag
import java.io.ByteArrayOutputStream

object ZigZagTests {
    fun generateKtZigZag(): MessageZigZag {
        val int = RandomGen.rnd.nextInt()
        val long = RandomGen.rnd.nextLong()

        return MessageZigZag.BuilderMessageZigZag(int, long).build()
    }

    fun generateMaxKtZigZag(): MessageZigZag {
        val int = Int.MAX_VALUE
        val long = Long.MAX_VALUE

        return MessageZigZag.BuilderMessageZigZag(int, long).build()
    }

    fun generateMinKtZigZag(): MessageZigZag {
        val int = Int.MIN_VALUE
        val long = Long.MIN_VALUE

        return MessageZigZag.BuilderMessageZigZag(int, long).build()
    }

    fun generateJvZigZag(): Zigzag.MessageZigZag {
        val int = RandomGen.rnd.nextInt()
        val long = RandomGen.rnd.nextLong()

        return Zigzag.MessageZigZag.newBuilder().setInt(int).setLong(long).build()
    }

    fun generateMaxJvZigZag(): Zigzag.MessageZigZag {
        val int = Int.MAX_VALUE
        val long = Long.MAX_VALUE

        return Zigzag.MessageZigZag.newBuilder().setInt(int).setLong(long).build()
    }

    fun generateMinJvZigZag(): Zigzag.MessageZigZag {
        val int = Int.MIN_VALUE
        val long = Long.MIN_VALUE

        return Zigzag.MessageZigZag.newBuilder().setInt(int).setLong(long).build()
    }

    fun compareZigZags(kt: MessageZigZag, jv: Zigzag.MessageZigZag): Boolean {
        return  kt.int == jv.int && kt.long == jv.long;
    }

    fun ktToJavaOnce() {
        val kt = generateKtZigZag()
        val outs = Util.getKtOutputStream(kt.getSizeNoTag())
        kt.writeTo(outs)

        val ins = Util.KtOutputStreamToInputStream(outs)
        val jv = Zigzag.MessageZigZag.parseFrom(ins)

        Util.assert(kt.errorCode == 0)
        Util.assert(compareZigZags(kt, jv))
    }

    fun ktToJavaOnceMaxValues() {
        val kt = generateMaxKtZigZag()
        val outs = Util.getKtOutputStream(kt.getSizeNoTag())
        kt.writeTo(outs)

        val ins = Util.KtOutputStreamToInputStream(outs)
        val jv = Zigzag.MessageZigZag.parseFrom(ins)

        Util.assert(kt.errorCode == 0)
        Util.assert(compareZigZags(kt, jv))
    }

    fun ktToJavaOnceMinValues() {
        val kt = generateMinKtZigZag()
        val outs = Util.getKtOutputStream(kt.getSizeNoTag())
        kt.writeTo(outs)

        val ins = Util.KtOutputStreamToInputStream(outs)
        val jv = Zigzag.MessageZigZag.parseFrom(ins)

        Util.assert(kt.errorCode == 0)
        Util.assert(compareZigZags(kt, jv))
    }

    fun jvToKtOnce() {
        val outs = ByteArrayOutputStream(10000000)

        val jv = generateJvZigZag()
        jv.writeTo(outs)
        val ins = CodedInputStream(outs.toByteArray())
        val kt = MessageZigZag.BuilderMessageZigZag(0, 0L).parseFrom(ins).build()

        Util.assert(kt.errorCode == 0)
        Util.assert(compareZigZags(kt, jv))
    }

    fun jvToKtOnceMaxValues() {
        val outs = ByteArrayOutputStream(10000000)

        val jv = generateMaxJvZigZag()
        jv.writeTo(outs)
        val ins = CodedInputStream(outs.toByteArray())
        val kt = MessageZigZag.BuilderMessageZigZag(0, 0L).parseFrom(ins).build()

        Util.assert(kt.errorCode == 0)
        Util.assert(compareZigZags(kt, jv))
    }

    fun jvToKtOnceMinValues() {
        val outs = ByteArrayOutputStream(10000000)

        val jv = generateMaxJvZigZag()
        jv.writeTo(outs)
        val ins = CodedInputStream(outs.toByteArray())
        val kt = MessageZigZag.BuilderMessageZigZag(0, 0L).parseFrom(ins).build()

        Util.assert(kt.errorCode == 0)
        Util.assert(compareZigZags(kt, jv))
    }

    val testRuns = 1000

    fun runTests() {
        for (i in 0..testRuns) {
            // random values
            jvToKtOnce()
            ktToJavaOnce()

            // max values
            jvToKtOnceMaxValues()
            ktToJavaOnceMaxValues()

            // min values
            jvToKtOnceMinValues()
            ktToJavaOnceMinValues()
        }
    }
}