internal open class Base internal constructor(name: String)

internal class One internal constructor(name: String, private val mySecond: String) : Base(name)