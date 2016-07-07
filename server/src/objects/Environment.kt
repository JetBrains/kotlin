package objects

/**
 * Created by user on 7/7/16.
 */
class Environment private constructor() {

    val map: MutableMap<String, Car>

    companion object {

        val instance = Environment()
    }

    init {
        map = mutableMapOf();
    }


}
