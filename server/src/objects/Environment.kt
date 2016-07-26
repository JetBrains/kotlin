package objects

import java.util.Random

/**
 * Created by user on 7/7/16.
 */
class Environment private constructor() {

    val map: MutableMap<Int, Car>

    companion object {

        val instance = Environment()
    }

    init {
        map = mutableMapOf();
    }

    @Synchronized
    fun connectCar(host: String, port: Int): Int {
        val uid = getNewUid()
        map.put(uid, Car(uid, host, port))
        return uid
    }

    fun getNewUid(): Int {
        var unique = false
        val random = Random()
        var uid: Int = 0
        while (!unique) {
            uid = random.nextInt(1000000)
            if (map.get(uid) == null) {
                unique = true;
            }
        }
        return uid;
    }

}
