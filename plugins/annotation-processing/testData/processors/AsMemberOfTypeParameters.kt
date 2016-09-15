annotation class Anno

@Anno
interface Intf<T>

open class Base<T> : Intf<T> {
    fun factory() = Base<CharSequence>()
}

class Impl : Base<String>()