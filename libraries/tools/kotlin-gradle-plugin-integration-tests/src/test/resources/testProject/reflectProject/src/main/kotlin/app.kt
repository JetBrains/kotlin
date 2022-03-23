import org.jetbrains.reflekt.Reflekt

fun main() {
    val a = Reflekt.objects().withAnnotations<B>(A::class).toList()
    println(a.toString())
    println("asd")
}