package receivers

class Receiver(val name: String) {
    fun own(): String = "own " + name
}
