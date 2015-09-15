internal open class Base internal constructor(o: Any, l: Int)

internal class C(private val string: String) : Base(string, string.length())
