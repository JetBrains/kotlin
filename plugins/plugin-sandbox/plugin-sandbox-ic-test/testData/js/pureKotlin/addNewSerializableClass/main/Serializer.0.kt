import org.jetbrains.kotlin.plugin.sandbox.CoreSerializer

@CoreSerializer
object Serializer

fun box(): String {
    Serializer.serializeA(A())
    return "OK"
}