// IS_APPLICABLE: false

class A

class AFabric {
    lateinit var instance: A
    fun init(config: Unit) {
        instance = A()
    }
}

class V(fabric: AFabric) {
    val a: A<caret>
    init {
        val config = getConfig()
        fabric.init(config)
        a = fabric.instance
    }
    fun getConfig() = Unit
}