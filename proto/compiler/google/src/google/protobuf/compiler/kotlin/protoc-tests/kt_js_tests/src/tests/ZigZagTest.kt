package tests
import MessageZigZag
import CodedInputStream

object ZigZagTest {
    val builder = protoBuf.loadProtoFile("./js_messages/zigzag.proto")
    val JSMessageZigZag = builder.build("MessageZigZag")

    fun generateKtZigZag(): MessageZigZag {
        val int = Util.nextInt()
        val long = Util.nextLong()

        return MessageZigZag.BuilderMessageZigZag(int, long.toLong()).build()
    }

    fun generateJsZigZag(): dynamic {
        val int = Util.nextInt()
        val long = Util.nextLong()
        val MessageClass = JSMessageZigZag
        return js("new MessageClass(int, long)")
    }

    fun compareZigZags(kt: MessageZigZag, jvs: dynamic): Boolean {
//        println("Kotlin message:")
//        printMessage(kt)
//        println()
//
//        println("JS Message:")
//        printMessage(jvs)
//        println()

        return kt.int == jvs.int &&
                kt.long.toString() == jvs.long.toString()
    }

    fun ktToJsOnce() {
        val kt = generateKtZigZag()
        val outs = Util.getKtOutputStream(kt.getSizeNoTag())
        kt.writeTo(outs)

        val jvs = JSMessageZigZag.decode(outs.buffer)

        Util.assert(kt.errorCode == 0)
        Util.assert(compareZigZags(kt, jvs))
    }

    fun jsToKtOnce() {
        val jvs = generateJsZigZag()
        val byteBuffer = jvs.toBuffer()

        val ins = CodedInputStream(Util.JSBufferToByteArray(byteBuffer))
        val kt = MessageZigZag.BuilderMessageZigZag(0, 0).parseFrom(ins).build()

        Util.assert(kt.errorCode == 0)
        Util.assert(compareZigZags(kt, jvs))
    }

    val testRuns = 1000

    fun runTests() {
        for (i in 0..testRuns) {
//            println("Kotlin -> JavaScript")
            ktToJsOnce()

//            println()
//            println("JavaScript -> Kotlin")
            jsToKtOnce()
        }
    }
}