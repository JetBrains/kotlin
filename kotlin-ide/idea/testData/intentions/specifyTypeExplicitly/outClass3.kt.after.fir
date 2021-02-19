open class F
class B<T>
class K<out T>

private fun check(): () -> B<out K<F>> = {
    class Local : F()
    B<K<Local>>()
}