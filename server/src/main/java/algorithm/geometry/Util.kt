package algorithm.geometry

object Util {


    fun degreesToRadian(angleDegrees: Int): Double {
        return Math.PI * angleDegrees / 180.0
    }

    fun radianToDegrees(angleRad: Double): Int {
        return (180.0 * angleRad / Math.PI).toInt()
    }

}