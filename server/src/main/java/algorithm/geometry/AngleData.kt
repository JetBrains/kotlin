package algorithm.geometry


class AngleData(val angle: Angle, val distance: Int) {

    fun toPoint(carAngleOX: Angle): Point {

        //convert to global coordinate system
        val angle = carAngleOX - angle
        val radianAngle = angle.rads()

        return Point(
                x = Math.cos(radianAngle) * distance,
                y = Math.sin(radianAngle) * distance
        )
    }

}