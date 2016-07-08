import client.Client
import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.*
import proto.car.RouteDoneP
import proto.car.RouteP.Route.WayPoint

/**
 * Created by user on 7/8/16.
 */
class ThisCar private constructor() {

    var id = 0


    //metr
    var x: Double = 0.toDouble()
    var y: Double = 0.toDouble()

    //Degree
    var angle: Double = 0.toDouble();//0 - положительное направление по OX

    val velocityMove: Double = 0.1//metr/sec
    val velocityRotation: Double = 10.toDouble()//Degree/sec

    val wayPoints: MutableList<WayPoint> = mutableListOf()
    var nextWayPointIndex = 0
    var pathDone = true

    var distanceToNext: Double = 0.toDouble()
    var angleToNext: Double = 0.toDouble()
    var anglePositive = true//true - поворот по часовой, false - против

    companion object {

        val instance = ThisCar()
    }


    @Synchronized
    fun move(deltaTime: Double) :Boolean {
        if (pathDone) {
            //маршрут не задан и сервер "в курсе", что маршрут пройден
            return false;
        }
        if (distanceToNext > 0 || angleToNext > 0) {
            if (angleToNext > 0) {
                //поворачиваемся
                val angleDelta: Double = velocityRotation * deltaTime

                angle += (if (anglePositive) 1 else -1) * angleDelta
                angleToNext -= angleDelta
                if (angleToNext < 0) {
                    angleToNext = 0.toDouble()
                }
            } else {
                //машинка повернута в нужную сторону, можно ехать
                var xDelta = deltaTime * velocityMove * Math.cos(angle * Math.PI / 180)
                var yDelta = deltaTime * velocityMove * Math.sin(angle * Math.PI / 180)
                x += xDelta
                y += yDelta
                distanceToNext -= deltaTime * velocityMove
                if (distanceToNext < 0) {
                    distanceToNext = 0.toDouble()
                }
            }
        } else if (nextWayPointIndex == wayPoints.size) {
            pathDone = true
            return true;
        } else {
            val wayPoint = wayPoints.get(nextWayPointIndex)
            angleToNext = Math.abs(wayPoint.angleDelta)
            distanceToNext = Math.abs(wayPoint.distance)//задний ход пока отключаем:)
            anglePositive = (wayPoint.angleDelta > 0)
            nextWayPointIndex++
        }
        return false;


    }

    @Synchronized
    fun setPath(wayPoints: List<WayPoint>) {
        println("in set path")
        this.pathDone = false
        distanceToNext = 0.toDouble()
        angleToNext = 0.toDouble()
        nextWayPointIndex = 0
        this.wayPoints.clear()
        this.wayPoints.addAll(wayPoints)
    }

    override fun toString(): String {
        return "x:$x; y:$y; angle:$angle"
    }
}
