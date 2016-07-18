annotation class Native(val type: String = "")

@Native
class classname_A(@Native("i32") val x: Int) {

    fun method(): Int {
        return this.x + 5
    }
}

fun simply_class_1(zz: Int) : Int{
    val x = classname_A(zz)
    val r = x.method()
    return r
}