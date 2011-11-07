enum Color : Runnable {
WHITE
BLACK
RED
YELLOW
BLUE
override public fun run() : Unit {
System.`out`?.println(("name()=" + name() + ", toString()=" + toString()))
}
}