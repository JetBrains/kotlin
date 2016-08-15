package tests

import java_msg.Direction
import main.kotlin.CodedInputStream
import main.kotlin.DirectionRequest
import java.io.ByteArrayOutputStream

object DirectionTest {
    fun generateKtDirectionRequest(): DirectionRequest {
        return DirectionRequest.BuilderDirectionRequest(
                DirectionRequest.Command.fromIntToCommand(RandomGen.rnd.nextInt(4))
            ).build()
    }

    fun generateJvDirectionRequest(): Direction.DirectionRequest {
        return Direction.DirectionRequest.newBuilder()
            .setCommandValue(RandomGen.rnd.nextInt(4)
            ).build()
    }

    fun compareDirectionRequests(kt: DirectionRequest, jv: Direction.DirectionRequest): Boolean {
        return kt.command.id == jv.command.number
    }

    val testRuns = 1000

    fun KtToJavaOnce() {
        val kt = generateKtDirectionRequest()
        val outs = Util.getKtOutputStream(kt.getSizeNoTag())
        kt.writeTo(outs)

        val ins = Util.KtOutputStreamToInputStream(outs)
        val jv = Direction.DirectionRequest.parseFrom(ins)

        Util.assert(kt.errorCode == 0)
        Util.assert(compareDirectionRequests(kt, jv))
    }

    fun KtToJava() {
        for (i in 0..testRuns) {
            KtToJavaOnce()
        }
    }

    fun JavaToKtOnce() {
        val outs = ByteArrayOutputStream(100000)

        val jv = generateJvDirectionRequest()
        jv.writeTo(outs)
        val ins = CodedInputStream(outs.toByteArray())
        val kt = generateKtDirectionRequest()
        kt.mergeFrom(ins)

        Util.assert(kt.errorCode == 0)
        Util.assert(compareDirectionRequests(kt, jv))
    }

    fun JavaToKT() {
        for (i in 0..testRuns) {
            JavaToKtOnce()
        }
    }

    fun runTests() {
        KtToJava()
        JavaToKT()
    }
}