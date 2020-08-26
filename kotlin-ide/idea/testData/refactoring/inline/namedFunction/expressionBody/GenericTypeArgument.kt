interface SomeFace
interface GeneOut<out T> {}
object Empty : GeneOut<Nothing>
fun <T> downUnder(): GeneOut<T> = Empty
fun downReturn<caret>(): GeneOut<SomeFace> = downUnder()
fun callDown() {
    val v1 = downReturn()
}