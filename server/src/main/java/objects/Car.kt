package objects


class Car constructor(uid: Int, host: String, port: Int) {

    val uid: Int
    val host: String
    val port: Int

    var lastAction: Long

    var x: Int
    var y: Int
    var angle: Int

    init {
        this.uid = uid
        this.host = host
        this.port = port
        x = 0
        y = 0
        angle = 0
        this.lastAction = System.currentTimeMillis()
    }

    override fun toString(): String {
        return "$uid ; x:$x; y:$y; target:$angle"
    }
}