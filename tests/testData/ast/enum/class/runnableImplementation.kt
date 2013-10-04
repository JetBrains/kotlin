enum class Color : Runnable {
WHITE
BLACK
RED
YELLOW
BLUE
public override fun run() : Unit {
System.out?.println("name()=" + name() + ", toString()=" + toString())
}
}