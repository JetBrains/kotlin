class Mocha() {
    operator fun invoke(x: Int, y: String, f: (Int) -> String) {
    }
}
fun main() {
    val mocha = Mocha()
    val testing = mocha<caret>(1, "fire"){ x: Int -> "hello world" }
}