class Test {
    val simple: String = "123"

    val inferType = simple.length.toString() + "4891"

    val getter: String = "O"
        get() = { field }() + "K"

    val constJavaClassValue: Class<*> = String::class.java
    val constClassValue: kotlin.reflect.KClass<*> = (String::class)

    @Deprecated("")
    var isBoolean = false

    @Deprecated("")
    var unit = Unit
}
