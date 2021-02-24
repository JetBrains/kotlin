import kotlin.reflect.KProperty

class Test {

    var broken by object {
        operator fun getValue(obj: Test, property: KProperty<*>) = Any()

        operator fun setValue(obj: Test, property: KProperty<*>, any: Any) {

        }
    }

    var overridden by object : java.io.Serializable {
        operator fun getValue(obj: Test, property: KProperty<*>) = Any()

        operator fun setValue(obj: Test, property: KProperty<*>, any: Any) {

        }
    }

    private val lazyProp by lazy {
        object : Runnable {
            override fun run() {}
        }
    }

}

var delegate by object {
    operator fun getValue(nothing: Nothing?, property: KProperty<*>) = Any()
    operator fun setValue(nothing: Nothing?, property: KProperty<*>, any: Any) {
        //empty
    }
}
