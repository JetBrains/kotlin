open class Base(name: String?) {}

open class One(name: String?, private var mySecond: String?) : Base(name) {}