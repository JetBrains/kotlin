import java.util.Objects

trait I

class C {
    fun foo1(i1: I, i2: I): Boolean {
        return i1 == i2
    }

    fun foo2(i1: I, i2: I): Boolean {
        return i1 != i2
    }
}