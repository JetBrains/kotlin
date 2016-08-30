package objects

object Environment {
    private var uid = 0
    private val onConnect = mutableListOf<(Car) -> Unit>()
    val map = mutableMapOf<Int, Car>()

    @Synchronized
    fun connectCar(host: String, port: Int): Int {
        uid++
        val car = Car(uid, host, port)
        onConnect.forEach { it(car) }
        map.put(uid, car)
        return uid
    }

    @Synchronized
    fun onCarConnect(callback: (Car) -> Unit) {
        onConnect.add(callback)
    }
}
