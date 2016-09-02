package objects


class Car constructor(val uid: Int, host: String, port: Int) {
    var x = 0.0
    var y = 0.0
    var angle = 0.0
    val carConnection = CarConnection(host, port)

    override fun toString(): String {
        return "$uid ; x:$x; y:$y; target:$angle"
    }
}