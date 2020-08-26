package test.pack.one

interface SomeFace
interface GeneOut<out T> {}
object Empty : GeneOut<Nothing>
fun <T> downUnder(): GeneOut<T> = Empty
fun downParameter<caret>(p: GeneOut<SomeFace>): GeneOut<SomeFace> {
    p
    return p
}
fun callDown() {
    val v2 = downParameter(downUnder())
}