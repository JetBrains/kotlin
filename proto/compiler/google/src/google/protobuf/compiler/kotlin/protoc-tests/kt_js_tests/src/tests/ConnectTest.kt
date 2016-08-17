package tests

import CodedInputStream
import ConnectionRequest

object ConnectTest {
    fun generateKotlinConnectionRequestMessage(): ConnectionRequest {
        val arr = Util.generateIntArray()
        val port = Util.nextInt()
        return ConnectionRequest.BuilderConnectionRequest(arr, port).build()
    }

    fun compareConnectionRequests(kt1: ConnectionRequest, kt2: ConnectionRequest): Boolean {
        return Util.compareArrays(kt1.ip.asIterable(), kt2.ip.asIterable()) &&
                kt1.port == kt2.port
    }

    fun ktToKtOnce() {
        val msg = generateKotlinConnectionRequestMessage()
        val outs = Util.getKtOutputStream(msg.getSizeNoTag())
        msg.writeTo(outs)

        val ins = CodedInputStream(outs.buffer)
        val readMsg = ConnectionRequest.BuilderConnectionRequest(IntArray(0), 0).parseFrom(ins).build()

        Util.assert(readMsg.errorCode == 0)
        Util.assert(compareConnectionRequests(msg, readMsg))
    }

    val testRuns = 1000

    fun runTests() {
        for (i in 0..testRuns) {
            ktToKtOnce()
        }
    }
}