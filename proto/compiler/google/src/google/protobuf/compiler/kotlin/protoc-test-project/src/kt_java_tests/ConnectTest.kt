package tests

import java_msg.Connect
import main.kotlin.CodedInputStream
import java.io.ByteArrayOutputStream

object ConnectTest {
    fun generateKotlinConnectionRequestMessage(): main.kotlin.ConnectionRequest {
        val arrSize = RandomGen.rnd.nextInt(1000)
        val arr = IntArray(arrSize)
        for (i in 0..(arrSize - 1)) {
            arr[i] = RandomGen.rnd.nextInt()
        }

        val port = RandomGen.rnd.nextInt()

        val msg = main.kotlin.ConnectionRequest.BuilderConnectionRequest(arr, port).build()

        return msg
    }


    fun generateJavaConnectionRequest(): Connect.ConnectionRequest {
        val arrSize = RandomGen.rnd.nextInt(1000)
        val arr = IntArray(arrSize)
        for (i in 0..(arrSize - 1)) {
            arr[i] = RandomGen.rnd.nextInt()
        }

        val port = RandomGen.rnd.nextInt()

        val msg = Connect.ConnectionRequest.newBuilder().addAllIp(arr.asIterable()).setPort(port).build()

        return msg
    }



    fun compareConnectionRequests(kt: main.kotlin.ConnectionRequest, jv: Connect.ConnectionRequest): Boolean {
        return Util.compareArrays(kt.ip.asIterable(), jv.ipList.asIterable())
    }


    fun ktToJavaOnce() {
        val ktConnectionRequest = generateKotlinConnectionRequestMessage()
        val outs = Util.getKtOutputStream(ktConnectionRequest.getSizeNoTag())
        ktConnectionRequest.writeTo(outs)

        val ins = Util.KtOutputStreamToInputStream(outs)
        val jvConnectionRequest = Connect.ConnectionRequest.parseFrom(ins)

        Util.assert(ktConnectionRequest.errorCode == 0)
        Util.assert(Util.compareArrays(ktConnectionRequest.ip.asIterable(), jvConnectionRequest.ipList))
        Util.assert(ktConnectionRequest.port == jvConnectionRequest.port)
    }

    val testRuns = 10

    fun KtToJava() {
        for (i in 0..testRuns) {
            ktToJavaOnce()
        }
    }

    fun JavaToKtOnce() {
        val outs = ByteArrayOutputStream(100000)

        val jvConnectionRequest = generateJavaConnectionRequest()
        jvConnectionRequest.writeTo(outs)
        val ins = CodedInputStream(outs.toByteArray())
        val ktConnectionRequest = main.kotlin.ConnectionRequest.BuilderConnectionRequest(IntArray(0), 0).parseFrom(ins)

        Util.assert(ktConnectionRequest.errorCode == 0)
        Util.assert(Util.compareArrays(ktConnectionRequest.ip.asIterable(), jvConnectionRequest.ipList))
        Util.assert(ktConnectionRequest.port == jvConnectionRequest.port)
    }

    fun JavaToKt() {
        for (i in 0..testRuns) {
            JavaToKtOnce()
        }
    }

    fun runTests() {
        KtToJava()
        JavaToKt()
    }
}