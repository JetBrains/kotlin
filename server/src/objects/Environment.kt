package objects

import java.util.*

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
    fun connectCar(host:String, port:Int):Int {
        //todo учетка памяти, тут машинки добавляются,но никогда не удаляются. Если включать и выключать одну и ту же машинку, то либо сервер зациклится, либо out of memory
        //todo в зависимости от того, что кончится раньше, память или айдишники:) Может сделать поток, который мониторит раз в N минут все машинки и дропает те, которые не активны более какого-то времени?
        var unique = false
        val random = Random()
        var uid:Int = 0
        while (!unique) {
            uid = random.nextInt(1000000)
            if (map.get(uid) == null) {
                unique = true;
            }
        }
        map.put(uid, Car(uid, host, port))
        return uid
    }


}
