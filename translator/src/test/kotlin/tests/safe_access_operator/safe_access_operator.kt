class save_access_operator_Gen(val i: Int)
class save_access_operator_MyClass(val i: save_access_operator_Gen?)

fun save_access_operator_test1(): Int {
    val x: save_access_operator_MyClass? = null
    val y = save_access_operator_MyClass(x?.i)

    if (y == null) {
        return 0
    } else {
        return 1
    }
}

