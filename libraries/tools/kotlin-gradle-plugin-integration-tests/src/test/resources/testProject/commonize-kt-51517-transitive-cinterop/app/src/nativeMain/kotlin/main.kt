import dummy.LENGTH
import dummy.Y
import dummy.foo
import kotlinx.cinterop.cValue
import yummy.sel
import yummy.yummy

fun nativeMain() {
    val y = cValue<Y> {
        n = 42
    }
    yummy(y)
    foo()
    sel(LENGTH)
    dummyMain()
}