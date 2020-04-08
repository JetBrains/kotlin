// IS_APPLICABLE: false

class A

class AFabric {
    lateinit var instance: A
    fun init(config: Unit) {
        instance = A()
    }
}

val fabric = AFabric()

class V {
    val a: A<caret>

    constructor() {
        val config = getConfig()
        fabric.init(config)
        a = fabric.instance
    }

    fun getConfig() = Unit
}