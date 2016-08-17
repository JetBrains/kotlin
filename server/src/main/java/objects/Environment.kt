package objects

import java.util.Random

/**
 * Created by user on 7/7/16.
 */
class Environment private constructor() {

    val map: MutableMap<Int, Car>
    private var uid = 0

    companion object {

        val instance = Environment()
    }

    init {
        map = mutableMapOf();
    }

    @Synchronized
    fun connectCar(host: String, port: Int): Int {
        uid++
        map.put(uid, Car(uid, host, port))
        return uid
    }
}
