package net.web.server.handlers

import CodedInputStream
import CodedOutputStream
import GenericResponse
import ModeChange
import Result
import net.Handler
import net.web.server.Server
import java.util.*

class ChangeMode : Handler {

    override fun execute(bytesFromClient: ByteArray): ByteArray {
        val ins = CodedInputStream(Base64.getDecoder().decode(bytesFromClient))
        val request = ModeChange.BuilderModeChange(ModeChange.Mode.fromIntToMode(0)).parseFrom(ins)
        val requestedMode = Server.ServerMode.fromProtoMode(request.newMode)

        if (Server.serverMode != Server.ServerMode.IDLE && requestedMode != Server.ServerMode.IDLE) {
            println("Can't change server mode from ${Server.serverMode.toString()} to ${requestedMode.toString()}")
            val protoResponse = GenericResponse.BuilderGenericResponse(Result.BuilderResult(1).build()).build()
            return protoBufToBytes(protoResponse)
        }

        // Change server mode
        Server.changeMode(requestedMode)

        // Respond with "OK"-protoBuf
        val protoResponse = GenericResponse.BuilderGenericResponse(Result.BuilderResult(0).build()).build()
        return protoBufToBytes(protoResponse)
    }

    private fun protoBufToBytes(protoMessage: GenericResponse): ByteArray {
        val result = ByteArray(protoMessage.getSizeNoTag())
        protoMessage.writeTo(CodedOutputStream(result))
        return result
    }
}