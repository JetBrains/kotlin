package net.car.server.handlers

import CodedInputStream
import CodedOutputStream
import ConnectionRequest
import ConnectionResponse
import net.Handler
import objects.Environment

class CarConnection : Handler {

    override fun execute(bytesFromClient: ByteArray): ByteArray {
        val data = ConnectionRequest.BuilderConnectionRequest(IntArray(0), 0).build()
        data.mergeFrom(CodedInputStream(bytesFromClient))
        val ipStr = data.ipValues.map { elem -> elem.toString() }.reduce { elem1, elem2 -> elem1 + "." + elem2 }
        val uid = Environment.connectCar(ipStr, data.port)
        val responseObject = ConnectionResponse.BuilderConnectionResponse(uid, 0).build()
        val result = ByteArray(responseObject.getSizeNoTag())
        responseObject.writeTo(CodedOutputStream(result))
        return result
    }
}