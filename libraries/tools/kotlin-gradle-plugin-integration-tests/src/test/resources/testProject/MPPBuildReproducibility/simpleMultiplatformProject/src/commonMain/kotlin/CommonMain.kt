fun main() {
    CommonMain.run()
}

object CommonMain {
    fun run() = println(commonMainExpect())
}

expect fun commonMainExpect(): String
