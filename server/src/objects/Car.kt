package objects

/**
 * Created by user on 7/7/16.
 */
class Car constructor(id: String, host: String, port: Int) {

    private val id: String
    private val host: String
    private val port: Int

    var free:Boolean

    init {
        this.id = id
        this.host = host
        this.port = port
        this.free = true
    }

}