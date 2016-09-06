package net.web.server.handlers

import CodedOutputStream
import DebugResponse
import algorithm.RoomModel
import net.Handler
import objects.Environment


class GetRoomModel : Handler {

    override fun execute(bytesFromClient: ByteArray): ByteArray {
        val protoMessage = getDebugInfo()
        val result = ByteArray(protoMessage.getSizeNoTag())
        protoMessage.writeTo(CodedOutputStream(result))
        return result
    }

    private fun getDebugInfo(): DebugResponse {

        val points = RoomModel.walls.flatMap({ it.points }).toTypedArray()

        val begin_x = IntArray(points.size)
        val begin_y = IntArray(points.size)
        val end_x = IntArray(points.size)
        val end_y = IntArray(points.size)

        val rawPoints = RoomModel.walls.flatMap({ it.rawPoints }).toTypedArray()
        val pointsX = rawPoints.map { it.x.toInt() }.toIntArray()
        val pointsY = rawPoints.map { it.y.toInt() }.toIntArray()

        for (i in 0..points.size - 2) {
            val curPoint = points[i]
            val nextPoint = points[i + 1]
            begin_x[i] = (curPoint.x + 0.5).toInt()
            begin_y[i] = (curPoint.y + 0.5).toInt()

            end_x[i] = (nextPoint.x + 0.5).toInt()
            end_y[i] = (nextPoint.y + 0.5).toInt()
        }

        val wallDistances = RoomModel.walls.filter { it.isFinished }.map {
            val firstPoint = it.points.first()
            val lastPoint = it.points.last()
            val vector = algorithm.geometry.Vector(firstPoint, lastPoint)
            vector.length().toInt()
        }.toIntArray()

        if (Environment.map.size == 0) {
            return DebugResponse.BuilderDebugResponse(begin_x, begin_y, end_x, end_y, 0, 0,
                    0, pointsX, pointsY, wallDistances).build()
        }
        val car = Environment.map.values.last()
        return DebugResponse.BuilderDebugResponse(begin_x, begin_y, end_x, end_y, car.x.toInt(), car.y.toInt(),
                car.angle.toInt(), pointsX, pointsY, wallDistances).build()
    }
}
