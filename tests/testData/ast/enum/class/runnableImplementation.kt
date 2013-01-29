enum class Color : Runnable {
WHITE
BLACK
RED
YELLOW
BLUE
public override fun run() : Unit {
System.out?.println("name()=" + name() + ", toString()=" + toString())
}
public fun name() : String { return "" }
public fun order() : Int { return 0 }
}