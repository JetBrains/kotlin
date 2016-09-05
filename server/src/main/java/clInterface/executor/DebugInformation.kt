package clInterface.executor

import CodedInputStream
import DebugRequest
import DebugResponseMemoryStats
import DebugResponseSonarStats
import roomScanner.serialize
import net.car.client.Client
import objects.Environment
import java.rmi.UnexpectedException

class DebugInformation : CommandExecutor {

    override fun execute(command: String) {
        val params = command.split(" ")
        val car = Environment.map[params[1].toInt()]!!
        val type = DebugRequest.Type.fromIntToType(params[2].toInt())

        val request = DebugRequest.BuilderDebugRequest(type).build()
        val requestType = when (type) {
            DebugRequest.Type.MEMORY_STATS -> Client.Request.DEBUG_MEMORY
            DebugRequest.Type.SONAR_STATS -> Client.Request.DEBUG_SONAR
            else -> throw UnexpectedException(type.toString())
        }

        val responseData = car.carConnection.sendRequest(
                requestType,
                serialize(request.getSizeNoTag(), { request.writeTo(it) })
        ).get().responseBodyAsBytes

        when (type) {
            DebugRequest.Type.MEMORY_STATS -> {
                val data = DebugResponseMemoryStats.BuilderDebugResponseMemoryStats(0, 0, 0, 0).parseFrom(CodedInputStream(responseData)).build()
                println("Heap static tail: ${data.heapStaticTail}")
                println("Heap dynamic tail: ${data.heapDynamicTail}")
                println("Heap dynamic max size: ${data.heapDynamicMaxBytes}")
                println("Heap dynamic total size: ${data.heapDynamicTotalBytes}")
            }
            DebugRequest.Type.SONAR_STATS -> {
                val data = DebugResponseSonarStats.BuilderDebugResponseSonarStats(0, 0, 0, 0).parseFrom(CodedInputStream(responseData)).build()
                println("Sonar measurement total: ${data.measurementCount}")
                println("Failed check sums: ${data.measurementFailedChecksum}")
                println("Failed command: ${data.measurementFailedCommand}")
                println("Failed distance: ${data.measurementFailedDistance}")
            }
            else -> throw UnexpectedException(type.toString())
        }
    }
}