class Vector(val x: Int, val y: Int) {
    // Binary operators
    operator fun plus(other: Vector): Vector = Vector(x + other.x, y + other.y)
    operator fun minus(other: Vector): Vector = Vector(x - other.x, y - other.y)
    operator fun times(scalar: Int): Vector = Vector(x * scalar, y * scalar)
    operator fun div(scalar: Int): Vector = Vector(x / scalar, y / scalar)
    
    // Unary operators
    operator fun unaryMinus(): Vector = Vector(-x, -y)
    
    // Comparison operators
    operator fun compareTo(other: Vector): Int = (x * x + y * y).compareTo(other.x * other.x + other.y * other.y)
    
    // Indexing operators
    operator fun get(index: Int): Int = when(index) {
        0 -> x
        1 -> y
        else -> throw IndexOutOfBoundsException("Invalid index: $index")
    }
    
    override fun toString(): String = "Vector($x, $y)"
}