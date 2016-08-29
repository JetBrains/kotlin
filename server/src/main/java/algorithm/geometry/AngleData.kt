package algorithm.geometry


class AngleData(val angle: Int, val distance: Int) {


    fun toPoint(carAngleOX: Int): Point {

        //convert to global coordinate system
        val angle = carAngleOX - angle
        val radianAngle = Util.degreesToRadian(angle)

        return Point(
                x = Math.cos(radianAngle) * distance,
                y = Math.sin(radianAngle) * distance
        )
    }

}