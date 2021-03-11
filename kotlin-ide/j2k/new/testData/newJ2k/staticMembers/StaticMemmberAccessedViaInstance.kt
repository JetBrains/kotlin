class Rectangle {
    var y = 0

    companion object {
        var x = 0
    }
}

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        val rectangle = Rectangle()
        Rectangle.x = 1
        rectangle.y = 2
    }
}
