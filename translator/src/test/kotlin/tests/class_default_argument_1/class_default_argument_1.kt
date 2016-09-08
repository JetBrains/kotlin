class class_default_argument_1_class(val classArg: Int) {
    fun getDefaultValue(arg1: Int, arg2: Int = 11): Int {
        return arg1 + arg2
    }
}

fun class_default_argument_1(z:Int):Int{
    val instance = class_default_argument_1_class(11)
    return instance.getDefaultValue(z)
}