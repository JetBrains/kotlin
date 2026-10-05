package receivers

open class Receiver(val name: String) {
    fun own(): String = "own " + name
}

class Derived(name: String) : Receiver(name)
