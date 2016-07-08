package objects

import car.client.Client

/**
 * Created by user on 7/7/16.
 */
class Car constructor(id: String, host: String, port: Int) {

    val id: String
    val host: String
    val port: Int

    var free: Boolean

    var x: Double
    var y: Double

    init {
        this.id = id
        this.host = host
        this.port = port
        this.free = true
        x = 0.toDouble()
        y = 0.toDouble()
    }

    override fun toString(): String {
        return "$id ; x:$x; y:$y"
    }
}