package objects.emulator

import SonarRequest
import SonarResponse
import algorithm.geometry.Angle
import algorithm.geometry.Line
import algorithm.geometry.Vector
import objects.Car
import org.asynchttpclient.ListenableFuture
import org.asynchttpclient.Response
import roomScanner.CarController
import roomScanner.CarController.Direction.*
import roomScanner.serialize

class CarEmulator(uid: Int, val testRoom: EmulatedRoom, val useRandom: Boolean, val randomGenerator: Rng) : Car(uid) {

    private var xReal = 0
    private var yReal = 0
    private var angleReal = Angle(0)

    override fun moveCar(distance: Int, direction: CarController.Direction): ListenableFuture<Response> {
        val coef = if (useRandom) randomGenerator.nextInt(900, 1100).toDouble() / 1000.0 else 1.0
        val distanceWithRandom = (coef * distance).toInt()
        when (direction) {
            FORWARD -> {
                xReal += (Math.cos(angleReal.rads()) * distanceWithRandom).toInt()
                yReal += (Math.sin(angleReal.rads()) * distanceWithRandom).toInt()
            }
            BACKWARD -> {
                xReal -= (Math.cos(angleReal.rads()) * distanceWithRandom).toInt()
                yReal -= (Math.sin(angleReal.rads()) * distanceWithRandom).toInt()
            }
            LEFT -> angleReal += Angle(distanceWithRandom)
            RIGHT -> angleReal -= Angle(distanceWithRandom)
        }
        println("x=$xReal y=$yReal angle=${angleReal.degs()}")
        return ListenableFutureImpl(ByteArray(0))
    }

    override fun scan(angles: IntArray, attempts: Int, windowSize: Int, smoothing: SonarRequest.Smoothing): ListenableFuture<Response> {


        val xSensor0 = xReal.toInt()
        val ySensor0 = yReal.toInt()
        val carAngle = angleReal

        val distances = arrayListOf<Int>()
        angles.forEach { angle ->
            val angleFinal = getSensorAngle(angle, carAngle.degs())
            val xSensor1: Int
            val ySensor1: Double
            //tg can be equal to inf if angle = 90 or 270. it vertical line. x1 = x0, y1 = y0+-[any number. eg 1]
            when (angleFinal) {
                90 -> {
                    xSensor1 = xSensor0
                    ySensor1 = (ySensor0 + 1).toDouble()
                }
                270 -> {
                    xSensor1 = xSensor0
                    ySensor1 = (ySensor0 - 1).toDouble()
                }
                in (90..270) -> {
                    xSensor1 = xSensor0 - 1
                    ySensor1 = ySensor0 + (xSensor1 - xSensor0) * Math.tan(angleFinal * Math.PI / 180)
                }
                else -> {
                    xSensor1 = xSensor0 + 1
                    ySensor1 = ySensor0 + (xSensor1 - xSensor0) * Math.tan(angleFinal * Math.PI / 180)
                }
            }
            val sensorLine = Line(ySensor0 - ySensor1, xSensor1.toDouble() - xSensor0,
                    xSensor0 * ySensor1 - ySensor0 * xSensor1)

            val sensorVector = Vector(xSensor0.toDouble(), ySensor0.toDouble(), xSensor1.toDouble(), ySensor1)
            val distance = getDistance(xSensor0, ySensor0, sensorLine, sensorVector)
            if (distance == -1) {
                distances.add(distance)
            } else {
                val delta = if (useRandom) randomGenerator.nextInt(-2, 2) else 0//return one of -2 -1 0 1 or 2
                distances.add(distance + delta)
            }
        }

        val responseMessage = SonarResponse.BuilderSonarResponse(distances.toIntArray()).build()
        val bytes = serialize(responseMessage.getSizeNoTag(), { responseMessage.writeTo(it) })

        return ListenableFutureImpl(bytes)
    }

    private fun getDistance(xSensor0: Int, ySensor0: Int, sensorLine: Line, sensorVector: Vector): Int {
        var result = Long.MAX_VALUE
        for (wall in testRoom.emulatedWalls) {
            val wallLine = wall.line
            val slope = sensorLine.A * wallLine.B - sensorLine.B * wallLine.A
            if (Math.abs(slope) < 0.01) {
                //line is parallel.
                continue
            }
            val xIntersection = (sensorLine.B * wallLine.C - wallLine.B * sensorLine.C) / slope
            val yIntersection = (sensorLine.C * wallLine.A - wallLine.C * sensorLine.A) / slope

            //filters by direction and intersection position
            val intersectionVector = Vector(xSensor0.toDouble(), ySensor0.toDouble(), xIntersection, yIntersection)
            if (intersectionVector.scalarProduct(sensorVector) < 0) {
                continue
            }
            val wallVector1 = Vector(xIntersection, yIntersection, wall.xTo.toDouble(), wall.yTo.toDouble())
            val wallVector2 = Vector(xIntersection, yIntersection, wall.xFrom.toDouble(), wall.yFrom.toDouble())
            if (wallVector1.scalarProduct(wallVector2) > 0) {
                continue
            }
            val currentDistance = Math.round(Math.sqrt(Math.pow(xIntersection - xSensor0, 2.0)
                    + Math.pow(yIntersection - ySensor0, 2.0)))
            if (currentDistance < result) {
                result = currentDistance
            }
        }
        return result.toInt()
    }


    private fun getSensorAngle(requestAngle: Int, carAngle: Int): Int {
        var angleTmp = carAngle - requestAngle
        while (angleTmp < 0) {
            angleTmp += 360
        }
        return angleTmp % 360
    }
}

