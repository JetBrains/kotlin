
class Gen(val i: Int)
class MyClass(val i: Gen?)

fun test1(): Int {
    val x: MyClass? = null
    val y = MyClass(x?.i)

    if (y == null) {
        return 0
    } else {
        return 1
    }
}

