// "Move to constructor" "true"
class Complex(x: Int, y: Double, z: String) {
    val <caret>y: Double = y // Duplicating
}