package tests
import MessageVarints
import CodedInputStream

object VarintsTest {
    val builder = protoBuf.loadProtoFile("./js_messages/varints.proto")
    val JSMessageVarints = builder.build("MessageVarints")
    val JSTestEnum = JSMessageVarints.TestEnum

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
                int, long.toLong(), sint, slong.toLong(), bl, enum, uint, ulong.toLong()
        ).build()
    }

    fun generateJsVarint(): dynamic {
        val int   = Util.nextInt()
        val long  = Util.nextLong()
        val sint  = Util.nextInt()
        val slong = Util.nextLong()
        val bl    = Util.nextBoolean()
        val uint  = Util.nextInt(0, Int.MAX_VALUE)
        val ulong = Util.nextLong(0, Long.MAX_VALUE)
        val enm  = JSTestEnum.firstVal
        val MessageClass = JSMessageVarints
        return js("new MessageClass(int, long, sint, slong, bl, uint, ulong, enm)")
    }

    fun compareVarints(kt: MessageVarints, jvs: dynamic): Boolean {
//        println("Kotlin message:")
//        printMessage(kt)
//        println()
//
//        println("JS Message:")
//        printMessage(jvs)
//        println()

        return kt.int == jvs.int &&
                kt.long.toString() == jvs.long.toString() &&
                kt.sint == jvs.sint &&
                kt.slong.toString() == jvs.slong.toString() &&
                kt.bl == jvs.bl &&
                Util.compareUints(kt.uint, jvs.uint) &&
                Util.compareUlongs(kt.ulong, jvs.ulong) &&
                kt.enumField.id == jvs.enumField
    }

    fun printMessage(msg: dynamic) {
        println("int = ${msg.int}\n" +
                "long = ${msg.long}\n" +
                "sint = ${msg.sint}\n" +
                "slong = ${msg.slong}\n" +
                "bl = ${msg.bl}\n" +
                "uint = ${msg.uint}\n" +
                "ulong = ${msg.ulong}\n" +
                "enumField = ${msg.enumField}\n")
    }

    fun ktToJsOnce() {
        val kt = generateKtVarint()
        val outs = Util.getKtOutputStream(kt.getSizeNoTag())
        kt.writeTo(outs)

        val jvs = JSMessageVarints.decode(outs.buffer)

        Util.assert(kt.errorCode == 0)
        Util.assert(compareVarints(kt, jvs))
    }

    fun jsToKtOnce() {
        val jvs = generateJsVarint()
        val byteBuffer = jvs.toBuffer()

        val ins = CodedInputStream(Util.JSBufferToByteArray(byteBuffer))
        val kt = MessageVarints.BuilderMessageVarints(
                0, 0, 0, 0, false, MessageVarints.TestEnum.firstVal, 0, 0
        ).parseFrom(ins).build()

//        println("Checking error in Kotlin message")
        Util.assert(kt.errorCode == 0)
//        println("Checking identity of serialization/deserialization")
        Util.assert(compareVarints(kt, jvs))
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