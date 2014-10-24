// ERROR: This type is final, so it cannot be inherited from
class Base(o: Any, l: Int)

class C(private val string: String) : Base(string, string.length())