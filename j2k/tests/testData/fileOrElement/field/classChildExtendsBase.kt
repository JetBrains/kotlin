// ERROR: This type is final, so it cannot be inherited from
class Base {
    private val myFirst: String? = null
}

class Child : Base() {
    private val mySecond: String? = null
}