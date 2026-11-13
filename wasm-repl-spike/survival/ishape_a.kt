// v1: Shape has one method. Sq instances get a v1 itable baked at allocation.
interface Shape {
    fun area(): Int
}

class Sq(val s: Int) : Shape {
    override fun area(): Int = s * s
}

fun makeSq(s: Int): Sq = Sq(s)
